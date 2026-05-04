package relish.relishTravel.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;
import relish.relishTravel.handler.ChargeManager;
import relish.relishTravel.message.MessageManager;
import relish.relishTravel.validator.SafetyValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChargeListener implements Listener {
    
    private final RelishTravel plugin;
    private final ChargeManager chargeManager;
    private final ConfigManager config;
    private final MessageManager messages;
    private final SafetyValidator safetyValidator;
    private final Map<UUID, JumpState> sneakingPlayers;
    private final Map<UUID, Long> lastJumpTime;
    private final Map<UUID, PendingTrigger> pendingTriggers;
    
    private enum JumpState {
        SNEAKING,
        JUMPED_SAFE,
        JUMPED_UNSAFE
    }

    private enum TriggerAction {
        SNEAK,
        JUMP,
        NONE
    }

    private static class PendingTrigger {
        TriggerAction firstAction;
        long firstTimeMs;

        PendingTrigger(TriggerAction firstAction, long firstTimeMs) {
            this.firstAction = firstAction;
            this.firstTimeMs = firstTimeMs;
        }
    }
    
    public ChargeListener(RelishTravel plugin, ChargeManager chargeManager,
                         ConfigManager config, MessageManager messages) {
        this.plugin = plugin;
        this.chargeManager = chargeManager;
        this.config = config;
        this.messages = messages;
        this.safetyValidator = new SafetyValidator(config);
        this.sneakingPlayers = new java.util.concurrent.ConcurrentHashMap<>();
        this.lastJumpTime = new java.util.concurrent.ConcurrentHashMap<>();
        this.pendingTriggers = new java.util.concurrent.ConcurrentHashMap<>();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (!config.isEnabled()) {
            return;
        }
        
        if (!player.hasPermission("relishtravel.use")) {
            return;
        }

        if (!plugin.isChargingEnabled(player)) {
            // If they disable charging while already sneaking/charging, keep things clean.
            if (!event.isSneaking()) {
                sneakingPlayers.remove(playerId);
                lastJumpTime.remove(playerId);
            }
            if (chargeManager.isCharging(player)) {
                chargeManager.cancelCharge(player);
            }
            return;
        }
        
        String worldName = player.getWorld().getName();
        if (config.getDisabledWorlds().contains(worldName) && 
            !player.hasPermission("relishtravel.bypass.disabled-worlds")) {
            if (event.isSneaking()) {
                messages.sendMessage(player, "world.disabled");
            }
            return;
        }
        
        if (!event.isSneaking()) {
            sneakingPlayers.remove(playerId);
            lastJumpTime.remove(playerId);
            pendingTriggers.remove(playerId);
            handleChargeRelease(player);
            return;
        }

        handleStartTriggerAction(player, TriggerAction.SNEAK);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!plugin.isChargingEnabled(player)) {
            return;
        }

        // Detect a jump (best-effort; Bukkit doesn't expose a dedicated jump event).
        if (!player.isSneaking() && getFirstAction() == TriggerAction.SNEAK) {
            // If the first action is sneak, we only care about jump while sneaking (unless second is NONE).
            // This keeps accidental jumps from triggering charge when not sneaking.
            return;
        }
        
        JumpState state = sneakingPlayers.get(playerId);
        
        if (chargeManager.isCharging(player)) {
            return;
        }
        
        if (event.getTo() != null && event.getFrom() != null) {
            if (state == null && player.getVelocity().getY() > 0.08) {
                long now = System.currentTimeMillis();
                Long lastJump = lastJumpTime.get(playerId);
                
                if (lastJump != null && now - lastJump < 500) {
                    return;
                }
                
                if (!player.isOnGround() && event.getFrom().getY() < event.getTo().getY() - 0.5) {
                    return;
                }
                
                sneakingPlayers.put(playerId, JumpState.JUMPED_SAFE);
                lastJumpTime.put(playerId, now);

                // Jump action for the trigger system.
                handleStartTriggerAction(player, TriggerAction.JUMP);
                return;
            }
            
            // Legacy SNEAK_JUMP behavior: start charging on landing after a sneak+jump.
            if (isLegacySneakJump() && state == JumpState.JUMPED_SAFE && player.isOnGround() && player.isSneaking()) {
                if (canStartChargeWithFeedback(player)) {
                    startChargeNow(player);
                }
                sneakingPlayers.put(playerId, JumpState.JUMPED_UNSAFE);
            }
        }
    }
    
    private boolean canStartChargeWithFeedback(Player player) {
        if (chargeManager.isCharging(player)) {
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Already charging - skipping");
            }
            return false;
        }

        // Charging is meant to be started from the ground. If the player is already gliding/flying,
        // don't start charging (prevents "moved" cancellations while in the air).
        if (player.isGliding() || player.isFlying() || !player.isOnGround()) {
            messages.sendMessage(player, "safety.already-flying");
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Cannot start charge while airborne/gliding");
            }
            return false;
        }
        
        if (!config.isEnabled()) {
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Plugin disabled");
            }
            return false;
        }
        
        if (!player.hasPermission("relishtravel.use")) {
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] No permission");
            }
            return false;
        }
        
        String worldName = player.getWorld().getName();
        if (config.getDisabledWorlds().contains(worldName) &&
            !player.hasPermission("relishtravel.bypass.disabled-worlds")) {
            messages.sendMessage(player, "world.disabled");
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] World disabled: " + worldName);
            }
            return false;
        }
        
        if (plugin.getLaunchHandler().isOnCooldown(player)) {
            long remainingMs = plugin.getLaunchHandler().getRemainingCooldownMs(player);
            String formattedTime = String.format("%.2fs", remainingMs / 1000.0);
            java.util.Map<String, String> placeholders = new java.util.HashMap<>();
            placeholders.put("time", formattedTime);
            messages.sendMessage(player, "launch.cooldown", placeholders);
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] On cooldown: " + formattedTime);
            }
            return false;
        }
        
        if (!safetyValidator.canStartCharge(player, messages)) {
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Failed safety checks");
            }
            return false;
        }
        
        if (!plugin.getElytraHandler().canUseElytra(player)) {
            messages.sendMessage(player, "safety.chest-slot-blocked");
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Cannot use Elytra");
            }
            return false;
        }
        
        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Safety checks passed - ready to charge");
        }
        
        return true;
    }
    
    private void startChargeNow(Player player) {
        chargeManager.startCharge(player, config.getChargeMaxTime());
    }
    
    private void handleChargeRelease(Player player) {
        if (!chargeManager.isCharging(player)) {
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Sneak released but not charging");
            }
            return;
        }
        
        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Sneak released - attempting launch");
        }
        
        boolean success = plugin.getLaunchHandler().executeLaunch(player);
        
        if (!success) {
            chargeManager.cancelCharge(player);
            plugin.getElytraHandler().cleanupFailedLaunch(player);
            player.sendActionBar(net.kyori.adventure.text.Component.empty());
            
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Launch FAILED - cleaned up");
            }
        } else {
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Launch SUCCESS");
            }
        }
    }

    private TriggerAction getFirstAction() {
        return parseAction(config.getChargeTriggerFirst(), TriggerAction.SNEAK);
    }

    private TriggerAction getSecondAction() {
        return parseAction(config.getChargeTriggerSecond(), TriggerAction.JUMP);
    }

    private boolean isLegacySneakJump() {
        // Legacy mode is active when config still uses charge.trigger: SNEAK_JUMP.
        // If the new keys are present, treat it as new behavior.
        if (config.getConfig() != null && (config.getConfig().contains("charge.trigger.first") || config.getConfig().contains("charge.trigger.second"))) {
            return false;
        }
        String legacy = config.getConfig() == null ? null : config.getConfig().getString("charge.trigger");
        return legacy != null && legacy.trim().equalsIgnoreCase("SNEAK_JUMP");
    }

    private TriggerAction parseAction(String raw, TriggerAction fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return TriggerAction.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private void handleStartTriggerAction(Player player, TriggerAction action) {
        if (action == TriggerAction.NONE) {
            return;
        }
        if (chargeManager.isCharging(player)) {
            return;
        }

        TriggerAction first = getFirstAction();
        TriggerAction second = getSecondAction();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        // If second is NONE, start immediately when first action happens.
        if (second == TriggerAction.NONE) {
            if (action == first) {
                if (canStartChargeWithFeedback(player)) {
                    startChargeNow(player);
                }
            }
            return;
        }

        // Two-step logic (also supports "double sneak" or "double jump" by using same action twice).
        PendingTrigger pending = pendingTriggers.get(id);
        long windowMs = 1200L;

        if (pending != null && now - pending.firstTimeMs > windowMs) {
            pendingTriggers.remove(id);
            pending = null;
        }

        if (pending == null) {
            if (action == first) {
                pendingTriggers.put(id, new PendingTrigger(first, now));
            }
            return;
        }

        // If first==second, require the same action twice.
        if (first == second) {
            if (action == first) {
                pendingTriggers.remove(id);
                if (canStartChargeWithFeedback(player)) {
                    startChargeNow(player);
                }
            }
            return;
        }

        if (pending.firstAction == first && action == second) {
            pendingTriggers.remove(id);
            if (canStartChargeWithFeedback(player)) {
                startChargeNow(player);
            }
        }
    }
}

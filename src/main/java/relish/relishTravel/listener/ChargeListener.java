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
    
    private enum JumpState {
        SNEAKING,
        JUMPED_SAFE,
        JUMPED_UNSAFE
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
            handleChargeRelease(player);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (!player.isSneaking()) {
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
                return;
            }
            
            if (state == JumpState.JUMPED_SAFE && player.isOnGround()) {
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
}

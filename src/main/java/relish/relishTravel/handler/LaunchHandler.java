package relish.relishTravel.handler;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;
import relish.relishTravel.message.MessageManager;
import relish.relishTravel.model.ChargeState;
import relish.relishTravel.model.LaunchData;
import relish.relishTravel.validator.SafetyValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LaunchHandler {
    
    private final RelishTravel plugin;
    private final ConfigManager config;
    private final MessageManager messages;
    private final ChargeManager chargeManager;
    private final ElytraHandler elytraHandler;
    private final SafetyValidator safetyValidator;
    
    private final Map<UUID, Long> cooldowns;
    private final Map<UUID, LaunchData> activeLaunches;
    private final Map<UUID, Integer> glideTaskIds;
    
    public LaunchHandler(RelishTravel plugin, ConfigManager config, MessageManager messages, 
                         ChargeManager chargeManager, ElytraHandler elytraHandler) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.chargeManager = chargeManager;
        this.elytraHandler = elytraHandler;
        this.safetyValidator = new SafetyValidator(config);
        this.cooldowns = new ConcurrentHashMap<>();
        this.activeLaunches = new ConcurrentHashMap<>();
        this.glideTaskIds = new ConcurrentHashMap<>();
    }
    
    public boolean executeLaunch(Player player) {
        ChargeState chargeState = chargeManager.getChargeState(player);
        if (chargeState == null) {
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] executeLaunch: No charge state found");
            }
            return false;
        }
        
        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] executeLaunch: Starting with " +
                                  (int)(chargeState.getChargePercent() * 100) + "% charge");
        }
        
        return executeLaunchWithPercent(player, chargeState.getChargePercent());
    }
    
    public boolean executeInstantLaunch(Player player, double chargePercent) {
        return executeLaunchWithPercent(player, chargePercent);
    }
    
    private boolean executeLaunchWithPercent(Player player, double chargePercent) {
        if (!safetyValidator.canLaunch(player, messages)) {
            return false;
        }
        
        if (!isChunkLoadedAhead(player)) {
            messages.sendMessage(player, "launch.chunk-not-loaded");
            return false;
        }
        
        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Attempting to equip Elytra...");
        }
        
        boolean elytraEquipped = elytraHandler.equipElytra(player);
        if (!elytraEquipped) {
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Elytra equip FAILED");
            }
            messages.sendMessage(player, "elytra.equip-failed");
            return false;
        }
        
        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Elytra equipped successfully");
        }
        
        double verticalPower = config.getMinPower() + (chargePercent * (config.getMaxPower() - config.getMinPower()));
        Vector direction = player.getLocation().getDirection().normalize();
        Vector velocity = direction.multiply(config.getForwardMomentum());
        velocity.setY(verticalPower * config.getVerticalBoost());
        velocity = capVelocity(velocity);
        player.setVelocity(velocity);
        long now = System.currentTimeMillis();
        long noFallUntil = now + (5 * 1000L);
        ItemStack originalChest = player.getInventory().getChestplate();
        boolean isVirtual = originalChest != null && originalChest.getItemMeta() != null && 
                           originalChest.getItemMeta().isUnbreakable();
        
        LaunchData launchData = new LaunchData(
            now,
            player.getLocation().getBlockY(),
            false,
            originalChest,
            isVirtual,
            noFallUntil,
            0,
            0L
        );
        
        activeLaunches.put(player.getUniqueId(), launchData);
        cooldowns.put(player.getUniqueId(), now + (config.getCooldownSeconds() * 1000L));
        
        if (config.isLaunchSoundEnabled()) {
            playLaunchSound(player);
        }
        if (plugin.getSpeedDisplayHandler() != null) {
            plugin.getSpeedDisplayHandler().startDisplay(player);
        }
        
        startGlideActivationTask(player);
        chargeManager.cancelCharge(player);
        return true;
    }
    
    public void applyForwardBoost(Player player) {
        LaunchData launchData = activeLaunches.get(player.getUniqueId());
        
        if (launchData != null && !launchData.hasForwardBoost() && player.isGliding()) {
            Vector velocity = player.getVelocity();
            Vector direction = player.getLocation().getDirection().normalize();
            velocity.add(direction.multiply(config.getForwardBoostSpeed()));
            velocity = capVelocity(velocity);
            player.setVelocity(velocity);
            activeLaunches.put(player.getUniqueId(), launchData.withForwardBoost());
            
            if (config.isForwardBoostSoundEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.5f);
            }
        }
    }
    
    public void handleGlideEnd(Player player) {
        UUID playerId = player.getUniqueId();
        LaunchData launchData = activeLaunches.remove(playerId);
        Integer taskId = glideTaskIds.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        if (plugin.getSpeedDisplayHandler() != null) {
            plugin.getSpeedDisplayHandler().stopDisplay(player);
        }
        
        if (launchData != null) {
            elytraHandler.removeElytra(player, launchData.isVirtualElytra());
        }
    }
    
    public boolean isOnCooldown(Player player) {
        if (player.hasPermission("relishtravel.bypass.cooldown")) {
            return false;
        }
        
        Long cooldownEnd = cooldowns.get(player.getUniqueId());
        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }
    
    public long getRemainingCooldownMs(Player player) {
        Long cooldownEnd = cooldowns.get(player.getUniqueId());
        if (cooldownEnd == null) {
            return 0;
        }
        
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    private long getRemainingCooldown(Player player) {
        return getRemainingCooldownMs(player) / 1000;
    }
    
    private Vector capVelocity(Vector velocity) {
        double maxHorizontal = config.getMaxHorizontalVelocity();
        double maxVertical = config.getMaxVerticalVelocity();
        
        if (Math.abs(velocity.getX()) > maxHorizontal) {
            velocity.setX(Math.signum(velocity.getX()) * maxHorizontal);
        }
        if (Math.abs(velocity.getZ()) > maxHorizontal) {
            velocity.setZ(Math.signum(velocity.getZ()) * maxHorizontal);
        }
        if (Math.abs(velocity.getY()) > maxVertical) {
            velocity.setY(Math.signum(velocity.getY()) * maxVertical);
        }
        
        return velocity;
    }
    
    public void cleanup() {
        for (Integer taskId : glideTaskIds.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        glideTaskIds.clear();
        activeLaunches.clear();
        cooldowns.clear();
    }
    
    private boolean isChunkLoadedAhead(Player player) {
        Location loc = player.getLocation();
        Vector direction = loc.getDirection().normalize().multiply(10);
        Location ahead = loc.clone().add(direction);
        
        return ahead.getChunk().isLoaded();
    }
    
    private void playLaunchSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
    }
    
    private void startGlideActivationTask(Player player) {
        UUID playerId = player.getUniqueId();
        Integer existingTaskId = glideTaskIds.get(playerId);
        if (existingTaskId != null) {
            Bukkit.getScheduler().cancelTask(existingTaskId);
        }
        
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int ticksRunning = 0;
            private final int maxTicks = 100;
            
            @Override
            public void run() {
                ticksRunning++;
                
                if (!player.isOnline() || !activeLaunches.containsKey(playerId)) {
                    glideTaskIds.remove(playerId);
                    Integer taskId = glideTaskIds.get(playerId);
                    if (taskId != null) {
                        Bukkit.getScheduler().cancelTask(taskId);
                    }
                    return;
                }
                
                LaunchData launchData = activeLaunches.get(playerId);
                int currentY = player.getLocation().getBlockY();
                int heightGained = currentY - launchData.launchY();
                if (player.isOnGround() && !player.isGliding()) {
                    handleGlideEnd(player);
                    Integer taskId = glideTaskIds.remove(playerId);
                    if (taskId != null) {
                        Bukkit.getScheduler().cancelTask(taskId);
                    }
                    return;
                }
                if (ticksRunning >= maxTicks && !player.isGliding()) {
                    handleGlideEnd(player);
                    Integer taskId = glideTaskIds.remove(playerId);
                    if (taskId != null) {
                        Bukkit.getScheduler().cancelTask(taskId);
                    }
                    return;
                }
                if (config.isAutoGlideEnabled() && !player.isGliding()) {
                    double yVelocity = player.getVelocity().getY();
                    if (yVelocity < -0.1 && heightGained > 5) {
                        player.setGliding(true);
                        
                        if (config.isAutoGlideEquipSoundEnabled()) {
                            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA, 1.0f, 1.0f);
                        }
                        
                        Integer taskId = glideTaskIds.remove(playerId);
                        if (taskId != null) {
                            Bukkit.getScheduler().cancelTask(taskId);
                        }
                        return;
                    }
                }
                if (heightGained < -3) {
                    if (player.isOnGround()) {
                        handleGlideEnd(player);
                        Integer taskId = glideTaskIds.remove(playerId);
                        if (taskId != null) {
                            Bukkit.getScheduler().cancelTask(taskId);
                        }
                        return;
                    }
                }
                if (player.isInWater() || player.isInLava()) {
                    handleGlideEnd(player);
                    Integer taskId = glideTaskIds.remove(playerId);
                    if (taskId != null) {
                        Bukkit.getScheduler().cancelTask(taskId);
                    }
                }
            }
        }, 5L, 2L); // Start after 5 ticks, check every 2 ticks
        
        glideTaskIds.put(playerId, task.getTaskId());
    }
    
    /**
     * Check if player should have fall damage prevention
     */
    public boolean shouldPreventFallDamage(Player player) {
        LaunchData launchData = activeLaunches.get(player.getUniqueId());
        return launchData != null && launchData.shouldPreventFallDamage();
    }
    
    /**
     * Check if player has an active launch
     */
    public boolean hasActiveLaunch(Player player) {
        return activeLaunches.containsKey(player.getUniqueId());
    }
    
    /**
     * Get active launch data for a player
     */
    public LaunchData getActiveLaunchData(Player player) {
        return activeLaunches.get(player.getUniqueId());
    }
    
    /**
     * Update launch data for a player
     */
    public void updateLaunchData(Player player, LaunchData data) {
        activeLaunches.put(player.getUniqueId(), data);
    }
}

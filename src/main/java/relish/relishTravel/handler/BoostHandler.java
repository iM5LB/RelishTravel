package relish.relishTravel.handler;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;
import relish.relishTravel.message.MessageManager;
import relish.relishTravel.model.LaunchData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BoostHandler {
    
    private final RelishTravel plugin;
    private final ConfigManager config;
    private final MessageManager messages;
    private final LaunchHandler launchHandler;
    private final Map<UUID, Long> normalElytraCooldowns;
    private final Map<UUID, Integer> normalElytraBoostCounts;
    
    public BoostHandler(RelishTravel plugin, ConfigManager config,
                        MessageManager messages, LaunchHandler launchHandler) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.launchHandler = launchHandler;
        this.normalElytraCooldowns = new ConcurrentHashMap<>();
        this.normalElytraBoostCounts = new ConcurrentHashMap<>();
    }
    
    public void applyBoost(Player player) {
        if (!config.isRightClickBoostEnabled()) {
            return;
        }
        
        if (!player.isGliding()) {
            return;
        }
        
        LaunchData launchData = launchHandler.getActiveLaunchData(player);
        
        if (launchData == null) {
            if (!config.isAllowBoostForNormalElytra()) {
                if (config.isDebugMode()) {
                    plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Boost denied - normal elytra boost disabled in config");
                }
                return;
            }
            
            UUID playerId = player.getUniqueId();
            long now = System.currentTimeMillis();
            Long cooldownEnd = normalElytraCooldowns.get(playerId);
            
            if (!player.hasPermission("relishtravel.bypass.boost-cooldown")) {
                if (cooldownEnd != null && now < cooldownEnd) {
                    long remainingMs = cooldownEnd - now;
                    String formattedTime = String.format("%.2fs", remainingMs / 1000.0);
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("time", formattedTime);
                    messages.sendMessage(player, "boost.cooldown", placeholders);
                    
                    if (config.isDebugMode()) {
                        plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Boost on cooldown (normal elytra): " + formattedTime);
                    }
                    return;
                }
            }
            
            int maxUses = getPlayerBoostLimit(player);
            int currentBoosts = normalElytraBoostCounts.getOrDefault(playerId, 0);
            
            if (maxUses >= 0 && currentBoosts >= maxUses) {
                messages.sendMessage(player, "boost.max-uses");
                
                if (config.isDebugMode()) {
                    plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Max boosts reached for normal elytra (" + maxUses + ")");
                }
                return;
            }
            
            applyBoostVelocity(player);
            
            long cooldownMillis = config.getRightClickBoostCooldown() * 1000L;
            normalElytraCooldowns.put(playerId, now + cooldownMillis);
            normalElytraBoostCounts.put(playerId, currentBoosts + 1);
            
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Boost applied (normal elytra) - Count: " + (currentBoosts + 1) + "/" + maxUses);
            }
            return;
        }
        
        if (!player.hasPermission("relishtravel.bypass.boost-cooldown")) {
            if (launchData.isRightClickBoostOnCooldown()) {
                long remainingMs = launchData.rightClickBoostCooldownUntil() - System.currentTimeMillis();
                String formattedTime = String.format("%.2fs", Math.max(0, remainingMs) / 1000.0);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("time", formattedTime);
                messages.sendMessage(player, "boost.cooldown", placeholders);
                
                if (config.isDebugMode()) {
                    plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Boost on cooldown (RelishTravel): " + formattedTime);
                }
                return;
            }
        }
        
        int maxUses = getPlayerBoostLimit(player);
        if (maxUses >= 0 && launchData.rightClickBoostCount() >= maxUses) {
            messages.sendMessage(player, "boost.max-uses");
            
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Max boosts reached (" + maxUses + ")");
            }
            return;
        }
        
        applyBoostVelocity(player);
        
        long cooldownMillis = config.getRightClickBoostCooldown() * 1000L;
        LaunchData updated = launchData.withRightClickBoost(cooldownMillis);
        launchHandler.updateLaunchData(player, updated);
        
        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Boost applied (RelishTravel launch)");
        }
    }
    
    public int getNormalElytraBoostCount(Player player) {
        return normalElytraBoostCounts.getOrDefault(player.getUniqueId(), 0);
    }
    
    public int getPlayerBoostLimit(Player player) {
        if (player.hasPermission("relishtravel.boost.unlimited")) {
            return -1;
        }
        
        Map<String, Integer> permissionLimits = config.getBoostPermissionLimits();
        int defaultLimit = config.getMaxBoostsPerGlide();
        boolean matchedAnyPermission = false;
        int highestLimit = Integer.MIN_VALUE;
        
        for (Map.Entry<String, Integer> entry : permissionLimits.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                matchedAnyPermission = true;
                int limit = entry.getValue();
                if (limit == -1) {
                    return -1;
                }
                if (limit > highestLimit) {
                    highestLimit = limit;
                }
            }
        }
        
        return matchedAnyPermission && highestLimit != Integer.MIN_VALUE ? highestLimit : defaultLimit;
    }
    
    private void applyBoostVelocity(Player player) {
        Vector velocity = player.getVelocity();
        Vector direction = player.getLocation().getDirection().normalize();
        
        velocity.add(direction.multiply(config.getRightClickBoostSpeed()));
        velocity = capVelocity(velocity);
        player.setVelocity(velocity);
        
        boolean soundEnabled = config.isBoostSoundEnabled();
        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Boost sound enabled: " + soundEnabled);
        }

        if (soundEnabled) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.8f);
        }
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
    
    public void clearCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        normalElytraCooldowns.remove(playerId);
        normalElytraBoostCounts.remove(playerId);
    }
    
    public void cleanup() {
        normalElytraCooldowns.clear();
        normalElytraBoostCounts.clear();
    }
}

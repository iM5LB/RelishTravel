package relish.relishTravel.handler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;
import relish.relishTravel.model.LaunchData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PersistenceHandler {
    
    private final RelishTravel plugin;
    private final ConfigManager config;
    private final LaunchHandler launchHandler;
    private final ElytraHandler elytraHandler;
    
    private static class PlayerFlightState {
        UUID playerId;
        Location location;
        Vector velocity;
        LaunchData launchData;
        ItemStack chestplate;
        boolean isGliding;
        long disconnectTime;
        
        PlayerFlightState(UUID playerId, Location location, Vector velocity, LaunchData launchData, 
                         ItemStack chestplate, boolean isGliding) {
            this.playerId = playerId;
            this.location = location.clone();
            this.velocity = velocity.clone();
            this.launchData = launchData;
            this.chestplate = chestplate != null ? chestplate.clone() : null;
            this.isGliding = isGliding;
            this.disconnectTime = System.currentTimeMillis();
        }
    }
    
    private final Map<UUID, PlayerFlightState> disconnectedStates = new ConcurrentHashMap<>();
    private static final long STATE_TIMEOUT_MS = 5 * 60 * 1000;
    
    public PersistenceHandler(RelishTravel plugin, ConfigManager config, LaunchHandler launchHandler, 
                             ElytraHandler elytraHandler) {
        this.plugin = plugin;
        this.config = config;
        this.launchHandler = launchHandler;
        this.elytraHandler = elytraHandler;
    }
    
    public void savePlayerState(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (!launchHandler.hasActiveLaunch(player)) {
            return;
        }
        
        LaunchData launchData = launchHandler.getActiveLaunchData(player);
        if (launchData == null) {
            return;
        }
        
        PlayerFlightState state = new PlayerFlightState(
            playerId,
            player.getLocation(),
            player.getVelocity(),
            launchData,
            player.getInventory().getChestplate(),
            player.isGliding()
        );
        
        disconnectedStates.put(playerId, state);
        
        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Saved flight state on disconnect");
        }
    }
    
    public void restorePlayerState(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerFlightState state = disconnectedStates.remove(playerId);
        
        if (state == null) {
            return;
        }
        
        long timeSinceDisconnect = System.currentTimeMillis() - state.disconnectTime;
        if (timeSinceDisconnect > STATE_TIMEOUT_MS) {
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Flight state expired (timeout)");
            }
            return;
        }
        
        player.teleport(state.location);
        player.setVelocity(state.velocity);
        
        if (state.chestplate != null) {
            player.getInventory().setChestplate(state.chestplate);
        }
        
        launchHandler.updateLaunchData(player, state.launchData);
        
        if (state.isGliding) {
            player.setGliding(true);
        }
        
        if (state.isGliding && plugin.getSpeedDisplayHandler() != null) {
            plugin.getSpeedDisplayHandler().startDisplay(player);
        }
        
        restartGlideActivationTask(player);
        
        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Restored flight state on join (was " + 
                                  (timeSinceDisconnect / 1000) + "s offline)");
        }
    }
    
    private void restartGlideActivationTask(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            
            if (!player.isGliding() && launchHandler.hasActiveLaunch(player)) {
                launchHandler.handleGlideEnd(player);
            }
        }, 20L);
    }
    
    public void cleanupExpiredStates() {
        long now = System.currentTimeMillis();
        disconnectedStates.entrySet().removeIf(entry -> 
            (now - entry.getValue().disconnectTime) > STATE_TIMEOUT_MS
        );
    }
    
    public void cleanup() {
        disconnectedStates.clear();
    }
}

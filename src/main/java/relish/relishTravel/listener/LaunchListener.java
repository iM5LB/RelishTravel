package relish.relishTravel.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;
import relish.relishTravel.handler.LaunchHandler;
import relish.relishTravel.message.MessageManager;

public class LaunchListener implements Listener {
    
    private final RelishTravel plugin;
    private final LaunchHandler launchHandler;
    private final ConfigManager config;
    private final MessageManager messages;
    
    public LaunchListener(RelishTravel plugin, LaunchHandler launchHandler, 
                         ConfigManager config, MessageManager messages) {
        this.plugin = plugin;
        this.launchHandler = launchHandler;
        this.config = config;
        this.messages = messages;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (!launchHandler.hasActiveLaunch(player) || !player.isGliding()) {
            return;
        }
        
        if (isMovingForward(player, event)) {
            launchHandler.applyForwardBoost(player);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        if (event.isGliding()) {
            if (plugin.getSpeedDisplayHandler() != null) {
                plugin.getSpeedDisplayHandler().startDisplay(player);
            }
        } else {
            if (plugin.getSpeedDisplayHandler() != null) {
                plugin.getSpeedDisplayHandler().stopDisplay(player);
            }
            
            if (plugin.getRightClickBoostHandler() != null) {
                plugin.getRightClickBoostHandler().clearCooldown(player);
            }
            
            if (launchHandler.hasActiveLaunch(player)) {
                launchHandler.handleGlideEnd(player);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        if (event.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
            if (config.isPreventKineticDamage()) {
                event.setCancelled(true);
                if (config.isDebugMode()) {
                    plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Prevented kinetic damage (FLY_INTO_WALL)");
                }
                return;
            }
        }
        
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (launchHandler.shouldPreventFallDamage(player)) {
                event.setCancelled(true);
                if (config.isDebugMode()) {
                    plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Prevented fall damage (active launch)");
                }
                return;
            }
            
            if (config.isPreventFallDamage() && player.isGliding()) {
                event.setCancelled(true);
                if (config.isDebugMode()) {
                    plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Prevented fall damage (config)");
                }
                return;
            }
        }
    }
    
    private boolean isMovingForward(Player player, PlayerMoveEvent event) {
        if (event.getFrom().equals(event.getTo())) {
            return false;
        }
        
        double yaw = Math.toRadians(player.getLocation().getYaw() + 90);
        double viewX = Math.cos(yaw);
        double viewZ = Math.sin(yaw);
        
        double moveX = event.getTo().getX() - event.getFrom().getX();
        double moveZ = event.getTo().getZ() - event.getFrom().getZ();
        
        double moveMagnitude = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (moveMagnitude < 0.01) {
            return false;
        }
        
        moveX /= moveMagnitude;
        moveZ /= moveMagnitude;
        
        double dotProduct = (viewX * moveX) + (viewZ * moveZ);
        return dotProduct > 0.7;
    }
}

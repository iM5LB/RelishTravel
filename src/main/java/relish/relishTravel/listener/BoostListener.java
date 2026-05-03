package relish.relishTravel.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;
import relish.relishTravel.handler.BoostHandler;

public class BoostListener implements Listener {
    
    private final RelishTravel plugin;
    private final BoostHandler boostHandler;
    private final ConfigManager config;
    
    public BoostListener(RelishTravel plugin, BoostHandler boostHandler) {
        this.plugin = plugin;
        this.boostHandler = boostHandler;
        this.config = plugin.getConfigManager();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        if (!event.isSneaking()) {
            return;
        }
        
        if (!player.isGliding()) {
            return;
        }
        
        if (player.isOnGround()) {
            return;
        }
        
        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Sneak boost triggered while gliding");
        }
        
        boostHandler.applyBoost(player);
    }
}

package relish.relishTravel.util;

import org.bukkit.entity.Player;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;

public class DebugLogger {
    
    private final RelishTravel plugin;
    private final ConfigManager config;
    
    public DebugLogger(RelishTravel plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }
    
    public void log(String message) {
        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
    
    public void log(Player player, String message) {
        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] " + message);
        }
    }
    
    public void sendToPlayer(Player player, String message) {
        if (config.isDebugMode() && player.hasPermission("relishtravel.debug")) {
            player.sendMessage("§7[DEBUG] " + message);
        }
    }
}

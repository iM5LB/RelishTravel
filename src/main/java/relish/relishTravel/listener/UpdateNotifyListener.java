package relish.relishTravel.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import relish.relishTravel.RelishTravel;

public class UpdateNotifyListener implements Listener {
    
    private final RelishTravel plugin;
    
    public UpdateNotifyListener(RelishTravel plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (player.hasPermission("relishtravel.admin")) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && plugin.getUpdateChecker() != null) {
                    plugin.getUpdateChecker().notifyPlayer(player);
                }
            }, 40L);
        }
    }
}

package relish.relishTravel.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.handler.PersistenceHandler;

public class PersistenceListener implements Listener {
    
    private final RelishTravel plugin;
    private final PersistenceHandler persistenceHandler;
    
    public PersistenceListener(RelishTravel plugin, PersistenceHandler persistenceHandler) {
        this.plugin = plugin;
        this.persistenceHandler = persistenceHandler;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        persistenceHandler.savePlayerState(player);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                persistenceHandler.restorePlayerState(player);
            }
        }, 1L);
    }
}

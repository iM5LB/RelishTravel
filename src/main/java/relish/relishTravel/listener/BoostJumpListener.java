package relish.relishTravel.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;
import relish.relishTravel.handler.BoostHandler;

/**
 * Handles boost triggered by pressing jump (space) while gliding.
 * Active when launch.boost.trigger is set to JUMP.
 *
 * While a player is gliding, Minecraft fires PlayerToggleFlightEvent
 * when they press the jump key — we intercept that to apply the boost.
 */
public class BoostJumpListener implements Listener {

    private final RelishTravel plugin;
    private final BoostHandler boostHandler;
    private final ConfigManager config;

    public BoostJumpListener(RelishTravel plugin, BoostHandler boostHandler) {
        this.plugin = plugin;
        this.boostHandler = boostHandler;
        this.config = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (!player.isGliding()) {
            return;
        }

        if (player.isOnGround()) {
            return;
        }

        // Only handle jump boosts when trigger mode is JUMP
        if (!"JUMP".equalsIgnoreCase(config.getBoostTrigger())) {
            return;
        }

        // Cancel the flight toggle so it doesn't interfere with gliding
        event.setCancelled(true);

        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Jump boost triggered while gliding");
        }

        boostHandler.applyBoost(player);
    }
}

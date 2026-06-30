package relish.relishTravel.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;
import relish.relishTravel.handler.BoostHandler;

/**
 * Handles boost triggered by left-click or right-click while gliding.
 * Active when launch.boost.trigger is set to LEFT_CLICK or RIGHT_CLICK.
 */
public class BoostClickListener implements Listener {

    private final RelishTravel plugin;
    private final BoostHandler boostHandler;
    private final ConfigManager config;

    public BoostClickListener(RelishTravel plugin, BoostHandler boostHandler) {
        this.plugin = plugin;
        this.boostHandler = boostHandler;
        this.config = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only fire once per click (main hand)
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();

        if (!player.isGliding()) {
            return;
        }

        if (player.isOnGround()) {
            return;
        }

        String trigger = config.getBoostTrigger();
        Action action = event.getAction();

        boolean isLeftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        boolean isRightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;

        if ("LEFT_CLICK".equals(trigger) && !isLeftClick) return;
        if ("RIGHT_CLICK".equals(trigger) && !isRightClick) return;
        if (!"LEFT_CLICK".equals(trigger) && !"RIGHT_CLICK".equals(trigger)) return;

        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] " + trigger + " boost triggered while gliding");
        }

        boostHandler.applyBoost(player);
    }
}

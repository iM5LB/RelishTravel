package relish.relishTravel.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.handler.LaunchHandler;

public class ElytraProtectionListener implements Listener {
    
    private final RelishTravel plugin;
    private final LaunchHandler launchHandler;
    
    public ElytraProtectionListener(RelishTravel plugin, LaunchHandler launchHandler) {
        this.plugin = plugin;
        this.launchHandler = launchHandler;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        if (!launchHandler.hasActiveLaunch(player)) {
            return;
        }
        
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        
        ItemStack chestplate = player.getInventory().getChestplate();
        
        if (chestplate != null && chestplate.getType() == Material.ELYTRA && isVirtualElytra(chestplate)) {
            if (event.getSlotType() == InventoryType.SlotType.ARMOR &&
                event.getSlot() == 38) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "elytra.cannot-remove-during-flight");
                return;
            }
            
            if (event.isShiftClick()) {
                ItemStack shiftClicked = event.getCurrentItem();
                if (shiftClicked != null && isChestplateItem(shiftClicked.getType())) {
                    event.setCancelled(true);
                    plugin.getMessageManager().sendMessage(player, "elytra.cannot-swap-during-flight");
                    return;
                }
            }
        }
        
        if (clicked != null && clicked.getType() == Material.ELYTRA && isVirtualElytra(clicked)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "elytra.cannot-remove-during-flight");
            return;
        }
        
        if (cursor != null && cursor.getType() == Material.ELYTRA && isVirtualElytra(cursor)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        if (!launchHandler.hasActiveLaunch(player)) {
            return;
        }
        
        ItemStack dragged = event.getOldCursor();
        if (dragged != null && dragged.getType() == Material.ELYTRA && isVirtualElytra(dragged)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        if (!launchHandler.hasActiveLaunch(player)) {
            return;
        }
        
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (dropped.getType() == Material.ELYTRA && isVirtualElytra(dropped)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "elytra.cannot-drop-during-flight");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        
        if (!launchHandler.hasActiveLaunch(player)) {
            return;
        }
        
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate != null && chestplate.getType() == Material.ELYTRA && isVirtualElytra(chestplate)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.ELYTRA || !isVirtualElytra(chestplate)) {
            return;
        }
        
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && isChestplateItem(item.getType())) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "elytra.cannot-swap-during-flight");
            }
        }
    }
    
    private boolean isChestplateItem(Material material) {
        String name = material.name();
        return name.endsWith("_CHESTPLATE") || material == Material.ELYTRA;
    }
    
    private boolean isVirtualElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.isUnbreakable();
    }
}

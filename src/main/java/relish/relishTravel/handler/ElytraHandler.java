package relish.relishTravel.handler;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ElytraHandler {
    
    private final RelishTravel plugin;
    private final ConfigManager config;
    private final Map<UUID, ItemStack> originalChestplates;
    
    public ElytraHandler(RelishTravel plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.originalChestplates = new ConcurrentHashMap<>();
    }
    
    public boolean canUseElytra(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        
        if (chest != null && chest.getType() == Material.ELYTRA) {
            return true;
        }
        
        if (config.isAutoEquipFromInventory()) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.ELYTRA && !isDamaged(item)) {
                    return true;
                }
            }
        }
        
        return config.isAllowVirtual();
    }
    
    public synchronized boolean equipElytra(Player player) {
        UUID playerId = player.getUniqueId();
        ItemStack chest = player.getInventory().getChestplate();
        
        if (chest != null && chest.getType() == Material.ELYTRA) {
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Already has Elytra equipped");
            }
            return true;
        }
        
        if (originalChestplates.containsKey(playerId)) {
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Cleaning up old entry before re-equipping");
            }
            originalChestplates.remove(playerId);
        }
        
        if (chest != null && chest.getType() != Material.AIR) {
            return false;
        }
        
        originalChestplates.put(playerId, new ItemStack(Material.AIR));
        
        if (config.isAutoEquipFromInventory()) {
            ItemStack elytraInInventory = findElytraInInventory(player);
            if (elytraInInventory != null) {
                player.getInventory().remove(elytraInInventory);
                player.getInventory().setChestplate(elytraInInventory);
                
                if (config.isDebugMode()) {
                    plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Equipped Elytra from inventory");
                }
                
                return true;
            }
        }
        
        if (config.isAllowVirtual()) {
            ItemStack virtualElytra = new ItemStack(Material.ELYTRA);
            ItemMeta meta = virtualElytra.getItemMeta();
            if (meta != null) {
                meta.setUnbreakable(true);
                meta.setDisplayName("§bRelishTravel Elytra");
                virtualElytra.setItemMeta(meta);
            }
            
            player.getInventory().setChestplate(virtualElytra);
            
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Equipped virtual Elytra");
            }
            
            return true;
        }
        
        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Failed to equip Elytra");
        }
        
        return false;
    }
    
    public synchronized void removeElytra(Player player, boolean wasVirtual) {
        UUID playerId = player.getUniqueId();
        ItemStack original = originalChestplates.remove(playerId);
        ItemStack currentChest = player.getInventory().getChestplate();
        
        if (wasVirtual) {
            if (currentChest != null && currentChest.getType() == Material.ELYTRA &&
                currentChest.getItemMeta() != null && currentChest.getItemMeta().isUnbreakable()) {
                if (original != null && original.getType() == Material.AIR) {
                    player.getInventory().setChestplate(null);
                } else {
                    player.getInventory().setChestplate(original);
                }
            }
        } else {
            if (currentChest != null && currentChest.getType() == Material.ELYTRA) {
                if (original != null) {
                    player.getInventory().setChestplate(original);
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(currentChest);
                    if (!leftover.isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), currentChest);
                    }
                }
            }
        }
    }
    
    private ItemStack findElytraInInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ELYTRA && !isDamaged(item)) {
                return item;
            }
        }
        return null;
    }
    
    private boolean isDamaged(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) {
            return true;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int maxDurability = item.getType().getMaxDurability();
            int damage = damageable.getDamage();
            return damage >= maxDurability;
        }
        
        return false;
    }
    
    public void restoreChestplate(Player player) {
        UUID playerId = player.getUniqueId();
        ItemStack original = originalChestplates.remove(playerId);
        
        if (original != null) {
            player.getInventory().setChestplate(original);
        }
    }
    
    public synchronized void cleanupFailedLaunch(Player player) {
        UUID playerId = player.getUniqueId();
        ItemStack original = originalChestplates.remove(playerId);
        
        if (original == null) {
            return;
        }
        
        ItemStack currentChest = player.getInventory().getChestplate();
        
        if (currentChest != null && currentChest.getType() == Material.ELYTRA) {
            boolean isVirtual = currentChest.getItemMeta() != null && 
                               currentChest.getItemMeta().isUnbreakable();
            
            if (isVirtual) {
                if (original.getType() == Material.AIR) {
                    player.getInventory().setChestplate(null);
                } else {
                    player.getInventory().setChestplate(original);
                }
            } else {
                player.getInventory().setChestplate(original);
                player.getInventory().addItem(currentChest);
            }
            
            if (config.isDebugMode()) {
                plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Cleaned up temp elytra after failed launch");
            }
        }
    }
    
    public void cleanup() {
        for (Map.Entry<UUID, ItemStack> entry : originalChestplates.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                player.getInventory().setChestplate(entry.getValue());
            }
        }
        originalChestplates.clear();
    }
}

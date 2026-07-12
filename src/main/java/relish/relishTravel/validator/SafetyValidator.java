package relish.relishTravel.validator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import relish.relishTravel.config.ConfigManager;
import relish.relishTravel.message.MessageManager;

public class SafetyValidator {
    
    private final ConfigManager config;
    
    public SafetyValidator(ConfigManager config) {
        this.config = config;
    }
    
    public boolean canStartCharge(Player player, MessageManager messages) {
        return canStartCharge(player, messages, false);
    }

    /**
     * @param skipGroundCheck if true, skip flying/gliding/on-ground checks entirely.
     *                        Used for JUMP_SNEAK where the player sneaks right after jumping
     *                        (can be mid-air or after landing).
     */
    public boolean canStartCharge(Player player, MessageManager messages, boolean skipGroundCheck) {
        if (!skipGroundCheck) {
            if (player.isFlying() || player.isGliding()) {
                // Silent: player is mid-air naturally (e.g. sneaking while gliding to boost).
                return false;
            }

            if (!player.isOnGround()) {
                // Silent: player is airborne but not flying/gliding.
                return false;
            }
        }
        
        if (player.isInWater()) {
            messages.sendMessage(player, "safety.in-water");
            return false;
        }
        
        if (player.isInLava()) {
            messages.sendMessage(player, "safety.in-lava");
            return false;
        }
        
        if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION)) {
            messages.sendMessage(player, "safety.has-levitation");
            return false;
        }
        
        if (!isChestSlotValid(player)) {
            messages.sendMessage(player, "safety.chest-slot-blocked");
            return false;
        }
        
        if (!hasSufficientVerticalSpace(player, messages)) {
            return false;
        }
        
        return true;
    }
    
    public boolean canLaunch(Player player, MessageManager messages) {
        if (player.isFlying() || player.isGliding()) {
            messages.sendMessage(player, "safety.already-flying");
            return false;
        }

        if (!player.isOnGround()) {
            messages.sendMessage(player, "safety.in-air");
            return false;
        }
        if (player.isInWater()) {
            messages.sendMessage(player, "safety.in-water");
            return false;
        }
        
        if (player.isInLava()) {
            messages.sendMessage(player, "safety.in-lava");
            return false;
        }
        
        if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION)) {
            messages.sendMessage(player, "safety.has-levitation");
            return false;
        }
        
        if (!isChestSlotValid(player)) {
            messages.sendMessage(player, "safety.chest-slot-blocked");
            return false;
        }
        
        if (!hasSufficientVerticalSpace(player, messages)) {
            return false;
        }
        
        return true;
    }
    
    private boolean isChestSlotValid(Player player) {
        var chest = player.getInventory().getChestplate();
        if (chest == null || chest.getType() == Material.AIR || chest.getType() == Material.ELYTRA) {
            return true;
        }
        // If auto-swap is enabled the chestplate will be moved aside at launch time.
        // Valid as long as there is somewhere to get an elytra (inventory, offhand, or virtual).
        if (config.isAutoSwapChestplate()) {
            if (config.isAllowVirtual()) {
                return true; // virtual elytra will be created regardless
            }
            if (config.isAutoEquipFromInventory()) {
                // Check storage inventory
                for (ItemStack item : player.getInventory().getStorageContents()) {
                    if (item != null && item.getType() == Material.ELYTRA) return true;
                }
                // Check offhand
                ItemStack offhand = player.getInventory().getItemInOffHand();
                if (offhand != null && offhand.getType() == Material.ELYTRA) return true;
            }
        }
        return false;
    }
    
    private boolean hasSufficientVerticalSpace(Player player, MessageManager messages) {
        Location loc = player.getLocation();
        int heightThreshold = config.getGlideHeightThreshold();
        
        for (int i = 1; i <= heightThreshold; i++) {
            Block block = loc.clone().add(0, i, 0).getBlock();
            Material material = block.getType();
            
            if (material.isAir()) {
                continue;
            }
            
            if (material.isSolid() && !block.isPassable()) {
                java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                placeholders.put("height", String.valueOf(heightThreshold));
                messages.sendMessage(player, "launch.obstruction", placeholders);
                return false;
            }
        }
        
        return true;
    }
    
    public boolean hasMovedBeyondTolerance(Location start, Location current, double tolerance) {
        double distance = Math.sqrt(
            Math.pow(current.getX() - start.getX(), 2) +
            Math.pow(current.getZ() - start.getZ(), 2)
        );
        
        return distance > tolerance;
    }
}

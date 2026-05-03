package relish.relishTravel.validator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import relish.relishTravel.config.ConfigManager;
import relish.relishTravel.message.MessageManager;

public class SafetyValidator {
    
    private final ConfigManager config;
    
    public SafetyValidator(ConfigManager config) {
        this.config = config;
    }
    
    public boolean canStartCharge(Player player, MessageManager messages) {
        if (player.isFlying()) {
            messages.sendMessage(player, "safety.already-flying");
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
    
    public boolean canLaunch(Player player, MessageManager messages) {
        return canStartCharge(player, messages);
    }
    
    private boolean isChestSlotValid(Player player) {
        var chest = player.getInventory().getChestplate();
        return chest == null || 
               chest.getType() == Material.AIR || 
               chest.getType() == Material.ELYTRA;
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

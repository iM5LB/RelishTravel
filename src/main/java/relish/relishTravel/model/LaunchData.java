package relish.relishTravel.model;

import org.bukkit.inventory.ItemStack;

public record LaunchData(
    long launchTime,
    int launchY,
    boolean hasForwardBoost,
    ItemStack originalChestplate,
    boolean isVirtualElytra,
    long noFallDamageUntil,
    int rightClickBoostCount,
    long rightClickBoostCooldownUntil
) {
    
    public boolean shouldPreventFallDamage() {
        return System.currentTimeMillis() < noFallDamageUntil;
    }
    
    public boolean isRightClickBoostOnCooldown() {
        return System.currentTimeMillis() < rightClickBoostCooldownUntil;
    }
    
    public int getRemainingCooldownSeconds() {
        long remaining = rightClickBoostCooldownUntil - System.currentTimeMillis();
        return Math.max(0, (int) (remaining / 1000));
    }
    
    public LaunchData withForwardBoost() {
        return new LaunchData(
            launchTime,
            launchY,
            true,
            originalChestplate,
            isVirtualElytra,
            noFallDamageUntil,
            rightClickBoostCount,
            rightClickBoostCooldownUntil
        );
    }
    
    public LaunchData withRightClickBoost(long cooldownMillis) {
        return new LaunchData(
            launchTime,
            launchY,
            hasForwardBoost,
            originalChestplate,
            isVirtualElytra,
            noFallDamageUntil,
            rightClickBoostCount + 1,
            System.currentTimeMillis() + cooldownMillis
        );
    }
}

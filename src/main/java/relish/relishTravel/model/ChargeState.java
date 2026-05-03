package relish.relishTravel.model;

import org.bukkit.Location;

public record ChargeState(
    Location startLocation,
    long startTime,
    double maxChargeTime,
    boolean isCharging
) {
    
    public double getChargePercent() {
        if (!isCharging) {
            return 0.0;
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        double seconds = elapsed / 1000.0;
        double percent = Math.min(seconds / maxChargeTime, 1.0);
        return percent;
    }
    
    public boolean isComplete() {
        return getChargePercent() >= 1.0;
    }
    
    public double getRemainingTime() {
        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
        return Math.max(0, maxChargeTime - elapsed);
    }
}

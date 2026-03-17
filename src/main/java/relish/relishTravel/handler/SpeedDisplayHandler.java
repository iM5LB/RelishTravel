package relish.relishTravel.handler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.message.MessageManager;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpeedDisplayHandler {
    
    private final RelishTravel plugin;
    private final MessageManager messages;
    private final LaunchHandler launchHandler;
    private final BoostHandler boostHandler;
    private final Set<UUID> displayingSpeed;
    private final Map<UUID, Integer> lastSpeedCache;
    private int taskId = -1;
    
    public SpeedDisplayHandler(RelishTravel plugin, MessageManager messages, 
                               LaunchHandler launchHandler, BoostHandler boostHandler) {
        this.plugin = plugin;
        this.messages = messages;
        this.launchHandler = launchHandler;
        this.boostHandler = boostHandler;
        this.displayingSpeed = ConcurrentHashMap.newKeySet();
        this.lastSpeedCache = new ConcurrentHashMap<>();
        startSpeedDisplayTask();
    }
    
    public void startDisplay(Player player) {
        displayingSpeed.add(player.getUniqueId());
    }
    
    public void stopDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        displayingSpeed.remove(playerId);
        lastSpeedCache.remove(playerId);
    }
    
    private void startSpeedDisplayTask() {
        int updateTicks = plugin.getConfigManager().getActionBarUpdateTicks();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (UUID playerId : displayingSpeed) {
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline() || !player.isGliding()) {
                    continue;
                }
                
                updateSpeedDisplay(player);
            }
        }, 0L, updateTicks);
    }
    
    private void updateSpeedDisplay(Player player) {
        Vector velocity = player.getVelocity();
        double speed = Math.sqrt(
            velocity.getX() * velocity.getX() +
            velocity.getY() * velocity.getY() +
            velocity.getZ() * velocity.getZ()
        ) * 20;
        
        int speedInt = (int) Math.abs(speed);
        UUID playerId = player.getUniqueId();
        
        Integer lastSpeed = lastSpeedCache.get(playerId);
        if (lastSpeed != null && Math.abs(speedInt - lastSpeed) < 2) {
            return;
        }
        lastSpeedCache.put(playerId, speedInt);
        
        // Build component directly without MiniMessage parsing for better performance
        Component message = Component.empty();
        
        if (plugin.getConfigManager().isBoostDisplayEnabled()) {
            int used = -1;
            int maxBoosts = -1;
            
            if (launchHandler.hasActiveLaunch(player)) {
                relish.relishTravel.model.LaunchData launchData = launchHandler.getActiveLaunchData(player);
                if (launchData != null) {
                    maxBoosts = boostHandler.getPlayerBoostLimit(player);
                    used = launchData.rightClickBoostCount();
                }
            } else if (player.isGliding() && plugin.getConfigManager().isAllowBoostForNormalElytra()) {
                maxBoosts = boostHandler.getPlayerBoostLimit(player);
                used = boostHandler.getNormalElytraBoostCount(player);
            }
            
            if (used != -1) {
                if (maxBoosts > 0) {
                    int remaining = Math.max(0, maxBoosts - used);
                    message = message.append(Component.text("Boosts: " + remaining + "/" + maxBoosts, NamedTextColor.AQUA))
                                   .append(Component.text(" | ", NamedTextColor.GRAY));
                } else if (maxBoosts == -1) {
                    // Unlimited boosts
                    message = message.append(Component.text("Boosts: ∞", NamedTextColor.AQUA))
                                   .append(Component.text(" | ", NamedTextColor.GRAY));
                } else {
                    message = message.append(Component.text("Boosts: " + used, NamedTextColor.AQUA))
                                   .append(Component.text(" | ", NamedTextColor.GRAY));
                }
            }
        }
        
        TextColor speedColor = getSpeedColor(speedInt);
        String speedBar = createSpeedBar(speedInt);
        
        message = message.append(Component.text("⚡ " + speedInt + " b/s " + speedBar, speedColor));
        
        player.sendActionBar(message);
    }
    
    private TextColor getSpeedColor(int speed) {
        if (speed < 10) return NamedTextColor.GREEN;
        if (speed < 20) return NamedTextColor.YELLOW;
        if (speed < 30) return NamedTextColor.GOLD;
        if (speed < 40) return NamedTextColor.RED;
        return NamedTextColor.DARK_RED;
    }
    
    private String createSpeedBar(int speed) {
        int bars = 10;
        int filled = Math.min(speed / 5, bars);
        
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            bar.append(i < filled ? "█" : "▁");
        }
        
        return bar.toString();
    }
    
    public void cleanup() {
        displayingSpeed.clear();
        lastSpeedCache.clear();
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}

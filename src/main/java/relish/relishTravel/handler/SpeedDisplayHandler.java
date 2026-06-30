package relish.relishTravel.handler;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
    @SuppressWarnings("unused")
    private final MessageManager messages;
    private final LaunchHandler launchHandler;
    private final BoostHandler boostHandler;

    private final Set<UUID> displaying;
    private final Map<UUID, Integer> lastSpeedCache;
    private final Map<UUID, Integer> lastHudStateCache;
    private final Map<UUID, BossBar> bossBars;

    private int taskId = -1;

    public SpeedDisplayHandler(RelishTravel plugin,
                               MessageManager messages,
                               LaunchHandler launchHandler,
                               BoostHandler boostHandler) {
        this.plugin = plugin;
        this.messages = messages;
        this.launchHandler = launchHandler;
        this.boostHandler = boostHandler;
        this.displaying = ConcurrentHashMap.newKeySet();
        this.lastSpeedCache = new ConcurrentHashMap<>();
        this.lastHudStateCache = new ConcurrentHashMap<>();
        this.bossBars = new ConcurrentHashMap<>();
        startTask();
    }

    public void startDisplay(Player player) {
        displaying.add(player.getUniqueId());
    }

    public void onConfigReload() {
        // Force a HUD refresh next tick for everyone currently tracked.
        lastSpeedCache.clear();
        lastHudStateCache.clear();

        // If HUD is OFF after reload, clear remnants immediately.
        HudMode mode = parseHudMode(plugin.getConfigManager().getHudMode());
        if (mode == HudMode.OFF) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != null && p.isOnline()) {
                    BossBar bar = bossBars.remove(p.getUniqueId());
                    if (bar != null) {
                        p.hideBossBar(bar);
                    }
                    clearActionBar(p);
                }
            }
            displaying.clear();
        }

        // Ensure the task is running (it will clear HUD while OFF).
        if (taskId == -1) {
            // Re-start if it was previously cancelled.
            startTask();
        }
    }

    public void stopDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        displaying.remove(playerId);
        lastSpeedCache.remove(playerId);
        lastHudStateCache.remove(playerId);

        BossBar bar = bossBars.remove(playerId);
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    private void startTask() {
        int updateTicks = plugin.getConfigManager().getActionBarUpdateTicks();
        if (updateTicks <= 0) {
            updateTicks = 4;
        }
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            // Safety net: ensure all currently gliding players are tracked, even if events were missed.
            HudMode mode = parseHudMode(plugin.getConfigManager().getHudMode());
            if (mode == HudMode.OFF) {
                // Hard-disable: clear any previously sent HUD for all online players.
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p != null && p.isOnline()) {
                        BossBar bar = bossBars.remove(p.getUniqueId());
                        if (bar != null) {
                            p.hideBossBar(bar);
                        }
                        // Action bar persists client-side until replaced; force-clear it.
                        clearActionBar(p);
                    }
                }
                displaying.clear();
                lastSpeedCache.clear();
                lastHudStateCache.clear();
                return;
            }

            if (mode != HudMode.OFF) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p != null && p.isOnline() && p.isGliding()) {
                        displaying.add(p.getUniqueId());
                    }
                }
            }

            for (UUID playerId : Set.copyOf(displaying)) {
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline() || !player.isGliding()) {
                    displaying.remove(playerId);
                    lastSpeedCache.remove(playerId);
                    lastHudStateCache.remove(playerId);

                    BossBar bar = bossBars.remove(playerId);
                    if (player != null && bar != null) {
                        player.hideBossBar(bar);
                    }
                    continue;
                }

                updateHud(player);
            }
        }, 0L, updateTicks);
    }

    private void updateHud(Player player) {
        HudMode mode = parseHudMode(plugin.getConfigManager().getHudMode());
        UUID playerId = player.getUniqueId();
        if (mode == HudMode.OFF) {
            // Fully disable glide HUD: hide any active bossbar and stop tracking the player.
            BossBar bar = bossBars.remove(playerId);
            if (bar != null) {
                player.hideBossBar(bar);
            }
            // Clear any previously-sent action bar text.
            clearActionBar(player);
            displaying.remove(playerId);
            lastSpeedCache.remove(playerId);
            lastHudStateCache.remove(playerId);
            return;
        }

        boolean showActionBar = mode == HudMode.ACTION_BAR;
        boolean showBossBar = mode == HudMode.BOSSBAR;

        Vector velocity = player.getVelocity();
        double speed = Math.sqrt(
            velocity.getX() * velocity.getX() +
            velocity.getY() * velocity.getY() +
            velocity.getZ() * velocity.getZ()
        ) * 20.0;

        int speedInt = (int) Math.abs(speed);
        int hudState = computeHudState(mode);

        Integer lastSpeed = lastSpeedCache.get(playerId);
        Integer lastHudState = lastHudStateCache.get(playerId);
        if (lastSpeed != null && Math.abs(speedInt - lastSpeed) < 2 && lastHudState != null && lastHudState == hudState) {
            return;
        }
        lastSpeedCache.put(playerId, speedInt);
        lastHudStateCache.put(playerId, hudState);

        BoostState boostState = computeBoostState(player);

        Component message = buildHudMessage(speedInt, boostState);
        if (message == null) {
            return;
        }

        if (showActionBar) {
            player.sendActionBar(message);
        }

        if (showBossBar) {
            BossBar bar = bossBars.computeIfAbsent(playerId, id -> {
                BossBar created = BossBar.bossBar(
                    message,
                    0.0f,
                    parseBossBarColor(plugin.getConfigManager().getBossBarColor()),
                    parseBossBarOverlay(plugin.getConfigManager().getBossBarOverlay())
                );
                player.showBossBar(created);
                return created;
            });

            bar.name(message);
            bar.progress(computeBossBarProgress(speed));
            bar.color(parseBossBarColor(plugin.getConfigManager().getBossBarColor()));
            bar.overlay(parseBossBarOverlay(plugin.getConfigManager().getBossBarOverlay()));
        } else {
            BossBar bar = bossBars.remove(playerId);
            if (bar != null) {
                player.hideBossBar(bar);
            }
        }
    }

    private int computeHudState(HudMode mode) {
        int result = 17;
        result = 31 * result + (mode == null ? 0 : mode.ordinal());
        result = 31 * result + (plugin.getConfigManager().isSpeedDisplayEnabled() ? 1 : 0);
        result = 31 * result + (plugin.getConfigManager().isBoostDisplayEnabled() ? 1 : 0);
        result = 31 * result + plugin.getConfigManager().getBossBarColor().hashCode();
        result = 31 * result + plugin.getConfigManager().getBossBarOverlay().hashCode();
        return result;
    }

    private Component buildHudMessage(int speedInt, BoostState boostState) {
        boolean showSpeed = plugin.getConfigManager().isSpeedDisplayEnabled();
        boolean showBoosts = plugin.getConfigManager().isBoostDisplayEnabled();

        Component message = Component.empty();
        boolean hasAnySection = false;

        if (showBoosts) {
            if (boostState != null && boostState.known) {
                if (boostState.unlimited) {
                    message = message
                        .append(Component.text("Boosts: \u221e", NamedTextColor.AQUA))
                        .append(Component.text(" | ", NamedTextColor.GRAY));
                    hasAnySection = true;
                } else if (boostState.max > 0) {
                    int remaining = Math.max(0, boostState.max - boostState.used);
                    message = message
                        .append(Component.text("Boosts: " + remaining + "/" + boostState.max, NamedTextColor.AQUA))
                        .append(Component.text(" | ", NamedTextColor.GRAY));
                    hasAnySection = true;
                } else {
                    message = message
                        .append(Component.text("Boosts: " + boostState.used, NamedTextColor.AQUA))
                        .append(Component.text(" | ", NamedTextColor.GRAY));
                    hasAnySection = true;
                }
            }
        }

        if (showSpeed) {
            TextColor speedColor = getSpeedColor(speedInt);
            String speedBar = createSpeedBar(speedInt);
            message = message.append(Component.text("\u26A1 " + speedInt + " b/s " + speedBar, speedColor));
            hasAnySection = true;
        }

        return hasAnySection ? message : null;
    }

    private BoostState computeBoostState(Player player) {
        int used = -1;
        int max = -1;
        boolean unlimited = false;

        if (launchHandler.hasActiveLaunch(player)) {
            relish.relishTravel.model.LaunchData launchData = launchHandler.getActiveLaunchData(player);
            if (launchData != null) {
                max = boostHandler.getPlayerBoostLimit(player);
                used = launchData.rightClickBoostCount();
            }
        } else if (plugin.getConfigManager().isAllowBoostForNormalElytra()) {
            max = boostHandler.getPlayerBoostLimit(player);
            used = boostHandler.getNormalElytraBoostCount(player);
        }

        if (used != -1 && max == -1) {
            unlimited = true;
        }

        return new BoostState(used != -1, unlimited, Math.max(0, used), max);
    }

    private record BoostState(boolean known, boolean unlimited, int used, int max) {}

    private float computeBossBarProgress(double speedBlocksPerSecond) {
        double maxH = plugin.getConfigManager().getMaxHorizontalVelocity();
        double maxV = plugin.getConfigManager().getMaxVerticalVelocity();
        double maxSpeed = Math.sqrt((maxH * maxH) + (maxV * maxV) + (maxH * maxH)) * 20.0;
        if (maxSpeed <= 0.0) {
            return 0.0f;
        }
        double progress = Math.max(0.0, Math.min(1.0, speedBlocksPerSecond / maxSpeed));
        return (float) progress;
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
            bar.append(i < filled ? "\u2588" : "\u2581");
        }

        return bar.toString();
    }

    private HudMode parseHudMode(String raw) {
        if (raw == null) {
            return HudMode.ACTION_BAR;
        }
        try {
            String upper = raw.trim().toUpperCase();
            if (upper.equals("BOTH") || upper.equals("ALL")) {
                return HudMode.ACTION_BAR;
            }
            return HudMode.valueOf(upper);
        } catch (IllegalArgumentException ignored) {
            return HudMode.ACTION_BAR;
        }
    }

    private void clearActionBar(Player player) {
        // Adventure should clear it, but on some server/client combos the last actionbar
        // can “stick” unless we also send a Spigot ACTION_BAR packet.
        player.sendActionBar(Component.empty());
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        } catch (Throwable ignored) {
            // Keep running on non-Spigot implementations or if the class isn't present.
        }
    }

    private BossBar.Color parseBossBarColor(String raw) {
        if (raw == null) {
            return BossBar.Color.BLUE;
        }
        try {
            return BossBar.Color.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return BossBar.Color.BLUE;
        }
    }

    private BossBar.Overlay parseBossBarOverlay(String raw) {
        if (raw == null) {
            return BossBar.Overlay.PROGRESS;
        }
        try {
            return BossBar.Overlay.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    private enum HudMode {
        ACTION_BAR,
        BOSSBAR,
        OFF
    }

    public void cleanup() {
        displaying.clear();
        lastSpeedCache.clear();
        lastHudStateCache.clear();
        bossBars.clear();
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}

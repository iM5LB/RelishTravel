package relish.relishTravel.handler;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.model.ChargeState;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChargeManager {

    private final RelishTravel plugin;
    private final Map<UUID, ChargeState> chargingPlayers;
    private final Map<UUID, Long> lastSoundTime;

    private final Map<UUID, BossBar> bossBars;

    private int taskId = -1;

    // Cached config values to avoid repeated lookups every tick
    private boolean actionBarEnabled;
    private boolean bossBarEnabled;
    private boolean soundsEnabled;
    private boolean particlesEnabled;

    public ChargeManager(RelishTravel plugin) {
        this.plugin = plugin;
        this.chargingPlayers = new ConcurrentHashMap<>();
        this.lastSoundTime = new ConcurrentHashMap<>();
        this.bossBars = new ConcurrentHashMap<>();
        updateConfigCache();
        startChargeUpdateTask();
    }

    public void updateConfigCache() {
        String hudMode = plugin.getConfigManager().getHudMode();
        String m = hudMode == null ? "ACTION_BAR" : hudMode.trim().toUpperCase();

        if (m.equals("BOTH") || m.equals("ALL")) {
            m = "ACTION_BAR";
        }

        this.actionBarEnabled = m.equals("ACTION_BAR");
        this.bossBarEnabled = m.equals("BOSSBAR");

        this.soundsEnabled = plugin.getConfigManager().isChargeSoundEnabled();
        this.particlesEnabled = plugin.getConfigManager().isParticlesEnabled();
    }

    public void startCharge(Player player, double maxChargeTime) {
        UUID playerId = player.getUniqueId();
        Location startLocation = player.getLocation().clone();
        long startTime = System.currentTimeMillis();

        ChargeState state = new ChargeState(startLocation, startTime, maxChargeTime, true);
        chargingPlayers.put(playerId, state);
    }

    public void cancelCharge(Player player) {
        UUID playerId = player.getUniqueId();
        chargingPlayers.remove(playerId);
        lastSoundTime.remove(playerId);

        BossBar bar = bossBars.remove(playerId);
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public boolean isCharging(Player player) {
        return chargingPlayers.containsKey(player.getUniqueId());
    }

    public ChargeState getChargeState(Player player) {
        return chargingPlayers.get(player.getUniqueId());
    }

    private void startChargeUpdateTask() {
        int updateTicks = plugin.getConfigManager().getActionBarUpdateTicks();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (UUID playerId : Set.copyOf(chargingPlayers.keySet())) {
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline()) {
                    chargingPlayers.remove(playerId);
                    lastSoundTime.remove(playerId);
                    bossBars.remove(playerId);
                    continue;
                }

                // If HUD is OFF, ensure any previously sent charge HUD is cleared and skip HUD updates.
                String hudMode = plugin.getConfigManager().getHudMode();
                boolean hudOff = hudMode != null && hudMode.trim().equalsIgnoreCase("OFF");
                if (hudOff) {
                    BossBar bar = bossBars.remove(playerId);
                    if (bar != null) {
                        player.hideBossBar(bar);
                    }
                    clearActionBar(player);
                }

                ChargeState state = chargingPlayers.get(playerId);
                if (state == null) {
                    continue;
                }

                if (!hudOff && actionBarEnabled) {
                    updateActionBar(player, state);
                }

                if (!hudOff && bossBarEnabled) {
                    updateBossBar(player, state);
                } else if (!hudOff) {
                    BossBar bar = bossBars.remove(playerId);
                    if (bar != null) {
                        player.hideBossBar(bar);
                    }
                }

                if (soundsEnabled) {
                    playSoundEffects(player, state);
                }

                if (particlesEnabled) {
                    spawnParticles(player, state);
                }
            }
        }, 0L, updateTicks);
    }

    private void updateActionBar(Player player, ChargeState state) {
        int percent = (int) (state.getChargePercent() * 100);
        String progressBar = createProgressBar(state.getChargePercent());

        Component message = Component.text()
            .append(Component.text("\u26A1 Charging: ", NamedTextColor.GOLD))
            .append(Component.text(percent + "%", NamedTextColor.YELLOW))
            .append(Component.text(" " + progressBar, NamedTextColor.YELLOW))
            .build();

        player.sendActionBar(message);
    }

    private void clearActionBar(Player player) {
        player.sendActionBar(Component.empty());
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        } catch (Throwable ignored) {
        }
    }

    private void updateBossBar(Player player, ChargeState state) {
        UUID playerId = player.getUniqueId();
        int percent = (int) (state.getChargePercent() * 100);
        Component title = Component.text("Charging: " + percent + "%", NamedTextColor.GOLD);

        BossBar bar = bossBars.computeIfAbsent(playerId, id -> {
            BossBar created = BossBar.bossBar(
                title,
                0.0f,
                parseBossBarColor(plugin.getConfigManager().getBossBarColor()),
                parseBossBarOverlay(plugin.getConfigManager().getBossBarOverlay())
            );
            player.showBossBar(created);
            return created;
        });

        bar.name(title);
        bar.progress((float) Math.max(0.0, Math.min(1.0, state.getChargePercent())));
        bar.color(parseBossBarColor(plugin.getConfigManager().getBossBarColor()));
        bar.overlay(parseBossBarOverlay(plugin.getConfigManager().getBossBarOverlay()));
    }


    private String createProgressBar(double percent) {
        int bars = 20;
        int filled = (int) (bars * percent);
        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < bars; i++) {
            bar.append(i < filled ? "\u2588" : "\u2581");
        }

        return bar.toString();
    }

    private void playSoundEffects(Player player, ChargeState state) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        Long lastSound = lastSoundTime.get(playerId);

        if (lastSound == null || now - lastSound >= 500) {
            try {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(plugin.getConfigManager().getSoundType());
                float pitchMin = plugin.getConfigManager().getSoundPitchMin();
                float pitchMax = plugin.getConfigManager().getSoundPitchMax();
                float pitch = pitchMin + (float) state.getChargePercent() * (pitchMax - pitchMin);
                float volume = plugin.getConfigManager().getSoundVolume();
                player.playSound(player.getLocation(), sound, volume, pitch);
                lastSoundTime.put(playerId, now);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound type: " + plugin.getConfigManager().getSoundType());
            }
        }
    }

    private void spawnParticles(Player player, ChargeState state) {
        try {
            org.bukkit.Particle particle = org.bukkit.Particle.valueOf(plugin.getConfigManager().getParticleType());
            Location loc = player.getLocation().add(0, 1, 0);

            double radius = 0.5 + state.getChargePercent() * 0.5;
            int particles = 5;

            for (int i = 0; i < particles; i++) {
                double angle = (2 * Math.PI * i) / particles;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);

                player.getWorld().spawnParticle(particle, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type: " + plugin.getConfigManager().getParticleType());
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

    public void cleanup() {
        chargingPlayers.clear();
        lastSoundTime.clear();
        bossBars.clear();
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}

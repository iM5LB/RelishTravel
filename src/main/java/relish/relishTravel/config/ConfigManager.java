package relish.relishTravel.config;

import org.bukkit.configuration.file.FileConfiguration;
import relish.relishTravel.RelishTravel;

import java.util.List;

public class ConfigManager {
    
    private final RelishTravel plugin;
    private FileConfiguration config;
    private final ConfigUpdater updater;
    
    public ConfigManager(RelishTravel plugin) {
        this.plugin = plugin;
        this.updater = new ConfigUpdater(plugin);
    }
    
    public void loadConfig() {
        updater.updateConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public boolean isEnabled() {
        return true;
    }
    
    public boolean isDebugMode() {
        return config != null && config.getBoolean("debug", false);
    }
    
    public double getChargeMaxTime() {
        return config.getDouble("charge.max-time", 2.5);
    }
    
    public boolean isCancelOnMove() {
        return config.getBoolean("charge.cancel-on-move", true);
    }
    
    public double getMinPower() {
        return config.getDouble("launch.min-power", 0.6);
    }
    
    public double getMaxPower() {
        return config.getDouble("launch.max-power", 1.4);
    }
    
    public double getForwardMomentum() {
        return config.getDouble("launch.forward-momentum", 0.3);
    }
    
    public double getVerticalBoost() {
        return config.getDouble("launch.vertical-boost", 1.5);
    }
    
    public int getCooldownSeconds() {
        return config.getInt("launch.cooldown-seconds", 5);
    }
    
    public boolean isAutoGlideEnabled() {
        return config.getBoolean("launch.auto-glide", true);
    }
    
    public double getForwardBoostSpeed() {
        return config.getDouble("launch.forward-boost-speed", 0.5);
    }
    
    public boolean isRightClickBoostEnabled() {
        return config.getBoolean("launch.boost.enabled", true);
    }
    
    public String getBoostTrigger() {
        return config.getString("launch.boost.trigger", "RIGHT_CLICK");
    }
    
    public double getRightClickBoostSpeed() {
        return config.getDouble("launch.boost.speed", 0.8);
    }
    
    public int getRightClickBoostCooldown() {
        return config.getInt("launch.boost.cooldown-seconds", 3);
    }
    
    public int getMaxBoostsPerGlide() {
        return config.getInt("launch.boost.default-limit", 3);
    }
    
    public java.util.Map<String, Integer> getBoostPermissionLimits() {
        java.util.Map<String, Integer> limits = new java.util.HashMap<>();
        org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("launch.boost.permission-limits");
        if (section == null) {
            return limits;
        }

        // Bukkit treats dots in YAML keys as path separators, so a key like
        // "relishtravel.boost.vip-plus" may be loaded as nested sections.
        // Flatten the tree back into permission nodes.
        flattenPermissionLimits(section, "", limits);
        return limits;
    }

    private void flattenPermissionLimits(org.bukkit.configuration.ConfigurationSection section,
                                         String prefix,
                                         java.util.Map<String, Integer> out) {
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (value instanceof org.bukkit.configuration.ConfigurationSection nested) {
                flattenPermissionLimits(nested, fullKey, out);
                continue;
            }

            if (value instanceof Number number) {
                out.put(fullKey, number.intValue());
            }
        }
    }
    
    public boolean requireEmptyHandForBoost() {
        return config.getBoolean("launch.boost.require-empty-hand", false);
    }
    
    public boolean isAllowBoostForNormalElytra() {
        return config.getBoolean("launch.boost.allow-for-normal-elytra", true);
    }
    
    public boolean isAllowVirtual() {
        return config.getBoolean("elytra.allow-virtual", true);
    }
    
    public boolean isPreventKineticDamage() {
        return config.getBoolean("elytra.prevent-kinetic-damage", true);
    }
    
    public boolean isPreventFallDamage() {
        return config.getBoolean("elytra.prevent-fall-damage", true);
    }
    
    public boolean isVirtualElytraVisible() {
        return true;
    }
    
    public boolean isAutoEquipFromInventory() {
        return config.getBoolean("elytra.auto-equip-from-inventory", true);
    }
    
    public List<String> getDisabledWorlds() {
        return config.getStringList("worlds.disabled-worlds");
    }
    
    public boolean isActionBarEnabled() {
        return true;
    }
    
    public boolean isSpeedDisplayEnabled() {
        return config.getBoolean("effects.speed-display", true);
    }
    
    public boolean isBoostDisplayEnabled() {
        return config.getBoolean("effects.boost-display", true);
    }
    
    public int getSpeedUpdateTicks() {
        return config.getInt("effects.action-bar-update-ticks", 4);
    }
    
    public int getActionBarUpdateTicks() {
        return config.getInt("effects.action-bar-update-ticks", 4);
    }
    
    public boolean isSoundsEnabled() {
        return config.getBoolean("effects.sounds", true);
    }
    
    public String getSoundType() {
        return config.getString("effects.sound-type", "BLOCK_BEACON_ACTIVATE");
    }
    
    public float getSoundVolume() {
        return (float) config.getDouble("effects.sound-volume", 0.5);
    }
    
    public float getSoundPitchMin() {
        return (float) config.getDouble("effects.sound-pitch-min", 0.5);
    }
    
    public float getSoundPitchMax() {
        return (float) config.getDouble("effects.sound-pitch-max", 2.0);
    }
    
    public boolean isParticlesEnabled() {
        return config.getBoolean("effects.particles", true);
    }
    
    public String getParticleType() {
        return config.getString("effects.particle-type", "ELECTRIC_SPARK");
    }
    
    public int getParticleCount() {
        return config.getInt("effects.particle-count", 5);
    }
    
    public double getParticleRadiusMin() {
        return config.getDouble("effects.particle-radius-min", 0.5);
    }
    
    public double getParticleRadiusMax() {
        return config.getDouble("effects.particle-radius-max", 1.0);
    }
    
    public boolean isLaunchSoundEnabled() {
        return config.getBoolean("effects.launch-sound-enabled", true);
    }
    
    public float getLaunchSoundVolume() {
        return (float) config.getDouble("effects.launch-sound-volume", 1.0);
    }
    
    public float getLaunchSoundPitch() {
        return (float) config.getDouble("effects.launch-sound-pitch", 1.5);
    }
    
    public double getMaxHorizontalVelocity() {
        return config.getDouble("velocity.max-horizontal", 3.0);
    }
    
    public double getMaxVerticalVelocity() {
        return config.getDouble("velocity.max-vertical", 2.0);
    }
    
    public int getGlideHeightThreshold() {
        return config.getInt("safety.glide-height-threshold", 5);
    }
    
    public boolean isBlockVanillaAchievements() {
        return config.getBoolean("achievements.block-vanilla-achievements", true);
    }
    
    public boolean isGrantRelishAchievement() {
        return config.getBoolean("achievements.grant-relish-achievement", true);
    }
    
    public String getCustomAchievementTitle() {
        return config.getString("achievements.custom-achievement.title", "Sky Traveler");
    }
    
    public String getCustomAchievementDescription() {
        return config.getString("achievements.custom-achievement.description", "Soar through the skies with RelishTravel. Sneak while gliding to boost!");
    }
    
    public String getLanguage() {
        return config.getString("language", "en");
    }
}

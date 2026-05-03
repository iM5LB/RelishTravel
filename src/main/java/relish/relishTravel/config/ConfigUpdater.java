package relish.relishTravel.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import relish.relishTravel.RelishTravel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConfigUpdater {
    
    private final RelishTravel plugin;
    private final File configFile;
    private static final int CURRENT_CONFIG_VERSION = 4;
    
    public ConfigUpdater(RelishTravel plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }
    
    public void updateConfig() {
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            return;
        }
        
        try {
            FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);
            int configVersion = oldConfig.getInt("config-version", 0);
            
            // Check if migration is needed
            if (configVersion < CURRENT_CONFIG_VERSION) {
                plugin.getLogger().info("Migrating config from version " + configVersion + " to " + CURRENT_CONFIG_VERSION);
                backupConfig();
                migrateConfig(oldConfig, configVersion);
                oldConfig.set("config-version", CURRENT_CONFIG_VERSION);
                oldConfig.save(configFile);
                plugin.getLogger().info("Config migration completed successfully!");
            } else if (configVersion > CURRENT_CONFIG_VERSION) {
                plugin.getLogger().warning("Config version (" + configVersion + ") is newer than plugin version (" + CURRENT_CONFIG_VERSION + ")");
                plugin.getLogger().warning("This may cause issues. Consider updating the plugin.");
            }
            
            // Merge any new keys from default config
            mergeNewKeys(oldConfig);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to update config: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    private void migrateConfig(FileConfiguration config, int fromVersion) {
        if (fromVersion < 2) {
            migrateToV2(config);
        }
        if (fromVersion < 3) {
            migrateToV3(config);
        }
        if (fromVersion < 4) {
            migrateToV4(config);
        }
    }

    private void migrateToV2(FileConfiguration config) {
        int moved = 0;

        // effects.sound-* -> effects.charge-sound.*
        moved += moveKeyIfMissing(config, "effects.sound-type", "effects.charge-sound.type");
        moved += moveKeyIfMissing(config, "effects.sound-volume", "effects.charge-sound.volume");
        moved += moveKeyIfMissing(config, "effects.sound-pitch-min", "effects.charge-sound.pitch-min");
        moved += moveKeyIfMissing(config, "effects.sound-pitch-max", "effects.charge-sound.pitch-max");

        if (moved > 0) {
            plugin.getLogger().info("Config migration v2: moved " + moved + " legacy key(s) to new structure");
        } else {
            plugin.getLogger().info("Config migration v2: no legacy keys found");
        }
    }

    private void migrateToV3(FileConfiguration config) {
        int moved = 0;

        // Drop multi-display mode values if present in old configs
        if (config.isString("effects.hud.mode")) {
            String mode = config.getString("effects.hud.mode", "ACTION_BAR");
            if (mode != null) {
                String upper = mode.trim().toUpperCase();
                if (upper.equals("ALL") || upper.equals("BOTH")) {
                    config.set("effects.hud.mode", "ACTION_BAR");
                    moved++;
                }
            }
        }

        if (moved > 0) {
            plugin.getLogger().info("Config migration v3: moved/cleaned " + moved + " key(s)");
        } else {
            plugin.getLogger().info("Config migration v3: no changes needed");
        }
    }

    private void migrateToV4(FileConfiguration config) {
        int moved = 0;

        // EXP bar HUD removed completely
        if (config.contains("effects.hud.expbar")) {
            config.set("effects.hud.expbar", null);
            moved++;
        }
        if (config.contains("effects.hud.expbar.enabled")) {
            config.set("effects.hud.expbar.enabled", null);
            moved++;
        }
        if (config.isString("effects.hud.mode")) {
            String mode = config.getString("effects.hud.mode", "ACTION_BAR");
            if (mode != null && mode.trim().equalsIgnoreCase("EXPBAR")) {
                config.set("effects.hud.mode", "ACTION_BAR");
                moved++;
            }
        }

        // Remove global sounds toggle if present (sound is now per-feature)
        if (config.isBoolean("effects.sounds") || config.isBoolean("effects.sounds.enabled")) {
            boolean enabled = config.getBoolean("effects.sounds", config.getBoolean("effects.sounds.enabled", true));

            // Preserve old intent: if global sounds was disabled, disable all sound categories unless user already set them.
            moved += setIfMissing(config, "effects.charge-sound.enabled", enabled);
            moved += setIfMissing(config, "effects.launch-sound-enabled", enabled);
            moved += setIfMissing(config, "launch.forward-boost-sound-enabled", enabled);
            moved += setIfMissing(config, "launch.auto-glide-equip-sound-enabled", enabled);
            moved += setIfMissing(config, "launch.boost.sound-enabled", enabled);
        }

        if (config.contains("effects.sounds")) {
            config.set("effects.sounds", null);
            moved++;
        }
        if (config.contains("effects.sounds.enabled")) {
            config.set("effects.sounds.enabled", null);
            moved++;
        }

        if (moved > 0) {
            plugin.getLogger().info("Config migration v4: removed " + moved + " legacy key(s)");
        } else {
            plugin.getLogger().info("Config migration v4: no changes needed");
        }
    }

    private int moveKeyIfMissing(FileConfiguration config, String fromPath, String toPath) {
        if (!config.contains(fromPath) || config.contains(toPath)) {
            return 0;
        }
        Object value = config.get(fromPath);
        config.set(toPath, value);
        config.set(fromPath, null);
        return 1;
    }

    private int setIfMissing(FileConfiguration config, String key, boolean value) {
        if (config.contains(key)) {
            return 0;
        }
        config.set(key, value);
        return 1;
    }
    
    private void mergeNewKeys(FileConfiguration oldConfig) {
        try {
            // Load default config from resources (UTF-8)
            InputStream in = plugin.getResource("config.yml");
            if (in == null) {
                plugin.getLogger().warning("Failed to load default config.yml from jar");
                return;
            }
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            
            boolean updated = mergeSection(defaultConfig, oldConfig, "");
            
            if (updated) {
                backupConfig();
                oldConfig.save(configFile);
                plugin.getLogger().info("Config updated with new keys!");
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to merge new config keys: " + e.getMessage());
        }
    }

    private boolean mergeSection(FileConfiguration sourceRoot,
                                 FileConfiguration targetRoot,
                                 String basePath) {
        boolean updated = false;

        org.bukkit.configuration.ConfigurationSection sourceSection =
            basePath.isEmpty() ? sourceRoot : sourceRoot.getConfigurationSection(basePath);
        org.bukkit.configuration.ConfigurationSection targetSection =
            basePath.isEmpty() ? targetRoot : targetRoot.getConfigurationSection(basePath);

        if (sourceSection == null) {
            return false;
        }

        if (!basePath.isEmpty() && targetSection == null) {
            targetSection = targetRoot.createSection(basePath);
            plugin.getLogger().info("Added new config section: " + basePath);
            updated = true;
        }

        for (String key : sourceSection.getKeys(false)) {
            String fullKey = basePath.isEmpty() ? key : basePath + "." + key;

            if (sourceSection.isConfigurationSection(key)) {
                updated |= mergeSection(sourceRoot, targetRoot, fullKey);
                continue;
            }

            if (!targetRoot.contains(fullKey)) {
                targetRoot.set(fullKey, sourceRoot.get(fullKey));
                plugin.getLogger().info("Added new config key: " + fullKey);
                updated = true;
            }
        }

        return updated;
    }
    
    private void backupConfig() throws IOException {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(configFile.getParentFile(), "config_backup_" + timestamp + ".yml");
        Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        plugin.getLogger().info("Config backed up to: " + backupFile.getName());
    }
}

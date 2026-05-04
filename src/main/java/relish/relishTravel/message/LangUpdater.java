package relish.relishTravel.message;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import relish.relishTravel.RelishTravel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Merges new keys from the jar language files into the server language files without overwriting existing values.
 * Note: Bukkit's YamlConfiguration does not preserve comments/formatting when saving.
 */
public class LangUpdater {

    private final RelishTravel plugin;

    public LangUpdater(RelishTravel plugin) {
        this.plugin = plugin;
    }

    public File ensureAndUpdate(String language) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            langFolder.mkdirs();
        }

        String lang = language == null ? "en" : language.trim();
        if (lang.isEmpty()) {
            lang = "en";
        }

        File langFile = new File(langFolder, lang + ".yml");

        // If missing, copy from jar (or fall back to en.yml).
        if (!langFile.exists()) {
            if (!copyFromJar(lang, langFile)) {
                plugin.getLogger().warning("Language file not found in jar: " + lang + ".yml, falling back to en.yml");
                langFile = new File(langFolder, "en.yml");
                if (!langFile.exists()) {
                    copyFromJar("en", langFile);
                }
            }
        }

        // Merge new keys from jar defaults into the server file.
        try {
            FileConfiguration serverCfg;
            try {
                serverCfg = loadUtf8(langFile);
            } catch (InvalidConfigurationException badYaml) {
                // The server file contains illegal YAML characters (control bytes etc.).
                // Back it up and restore a clean copy from the jar so the plugin can start.
                plugin.getLogger().warning("Invalid language YAML detected (" + langFile.getName() + "): " + badYaml.getMessage());
                backup(langFile);
                if (!copyFromJar(lang, langFile) && !langFile.getName().equalsIgnoreCase("en.yml")) {
                    copyFromJar("en", langFile);
                }
                serverCfg = loadUtf8(langFile);
            }

            // Small message migrations: only rewrite if the server file still has our old default strings.
            migrateLegacyToggleStrings(serverCfg);

            String jarLang = langFile.getName().equalsIgnoreCase("en.yml") ? "en" : lang;
            InputStream in = plugin.getResource("lang/" + jarLang + ".yml");
            if (in == null) {
                // If the selected language isn't packaged, use en.yml as the key source.
                in = plugin.getResource("lang/en.yml");
            }
            if (in == null) {
                plugin.getLogger().warning("Failed to load language defaults from jar (lang/en.yml missing)");
                return langFile;
            }

            FileConfiguration defaultCfg = loadResourceYamlSanitized(in);

            int added = mergeMissingLeaves(defaultCfg, serverCfg);
            if (added > 0) {
                backup(langFile);
                serverCfg.save(langFile);
                plugin.getLogger().info("Language file updated with " + added + " new key(s): " + langFile.getName());
            }
        } catch (Throwable e) {
            // Never fail plugin enable due to a language file problem.
            plugin.getLogger().warning("Failed to update language file: " + langFile.getName() + " (" + e.getMessage() + ")");
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }

        return langFile;
    }

    private void migrateLegacyToggleStrings(FileConfiguration serverCfg) {
        // Previous default strings used "OK/OFF". Replace them with the standard icons used elsewhere,
        // but only if the value matches the old defaults to avoid overwriting custom translations.
        String enabledKey = "messages.command.toggle.enabled";
        String disabledKey = "messages.command.toggle.disabled";

        String enabled = serverCfg.getString(enabledKey, "");
        if ("<green>OK <gray>Charging enabled.".equals(enabled)) {
            serverCfg.set(enabledKey, "<green>âœ” <gray>Charging enabled.");
        }

        String disabled = serverCfg.getString(disabledKey, "");
        if ("<yellow>OFF <gray>Charging disabled.".equals(disabled)) {
            serverCfg.set(disabledKey, "<red>âœ– <gray>Charging disabled.");
        }
    }

    private FileConfiguration loadResourceYamlSanitized(InputStream in) throws IOException, InvalidConfigurationException {
        byte[] bytes = in.readAllBytes();
        String raw = new String(bytes, StandardCharsets.UTF_8);
        String sanitized = sanitizeYaml(raw);

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.loadFromString(sanitized);
        return cfg;
    }

    private boolean copyFromJar(String language, File outFile) {
        try (InputStream in = plugin.getResource("lang/" + language + ".yml")) {
            if (in == null) {
                return false;
            }
            Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to copy language file from jar: " + language + ".yml (" + e.getMessage() + ")");
            return false;
        }
    }

    private FileConfiguration loadUtf8(File file) throws IOException, InvalidConfigurationException {
        // Bukkit's loadConfiguration(Reader) may throw InvalidConfigurationException on illegal code points.
        // Read the whole file, sanitize illegal control chars, then load from string.
        byte[] bytes = Files.readAllBytes(file.toPath());
        String raw = new String(bytes, StandardCharsets.UTF_8);
        String sanitized = sanitizeYaml(raw);

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.loadFromString(sanitized);
        return cfg;
    }

    private String sanitizeYaml(String in) {
        // Strip C0/C1 control chars that SnakeYAML rejects (except common whitespace).
        // Keep: \r \n \t
        StringBuilder sb = new StringBuilder(in.length());
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c == '\r' || c == '\n' || c == '\t') {
                sb.append(c);
                continue;
            }
            if (c < 0x20) {
                continue;
            }
            if (c >= 0x7F && c <= 0x9F) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private int mergeMissingLeaves(FileConfiguration sourceRoot, FileConfiguration targetRoot) {
        // Use a leaf-key merge to avoid Bukkit edge-cases with section detection.
        int added = 0;

        java.util.Map<String, Object> source = sourceRoot.getValues(true);
        java.util.Map<String, Object> target = targetRoot.getValues(true);

        for (var entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof ConfigurationSection) {
                continue;
            }

            // If the key is missing, add it. We intentionally do NOT overwrite custom server values.
            boolean exists = target.containsKey(key);
            if (!exists) {
                targetRoot.set(key, value);
                added++;
            }
        }

        return added;
    }

    private void backup(File langFile) throws IOException {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(langFile.getParentFile(),
            langFile.getName().replace(".yml", "") + "_backup_" + timestamp + ".yml");
        Files.copy(langFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}

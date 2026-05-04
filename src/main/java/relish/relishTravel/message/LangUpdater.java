package relish.relishTravel.message;

import org.bukkit.configuration.ConfigurationSection;
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
            FileConfiguration serverCfg = loadUtf8(langFile);

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

            FileConfiguration defaultCfg = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));

            boolean updated = mergeMissing(defaultCfg, serverCfg, "");
            if (updated) {
                backup(langFile);
                serverCfg.save(langFile);
                plugin.getLogger().info("Language file updated with new keys: " + langFile.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update language file: " + langFile.getName() + " (" + e.getMessage() + ")");
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }

        return langFile;
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

    private FileConfiguration loadUtf8(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }

    private boolean mergeMissing(FileConfiguration sourceRoot, FileConfiguration targetRoot, String basePath) {
        boolean updated = false;

        ConfigurationSection sourceSection =
            basePath.isEmpty() ? sourceRoot : sourceRoot.getConfigurationSection(basePath);
        ConfigurationSection targetSection =
            basePath.isEmpty() ? targetRoot : targetRoot.getConfigurationSection(basePath);

        if (sourceSection == null) {
            return false;
        }

        if (!basePath.isEmpty() && targetSection == null) {
            targetSection = targetRoot.createSection(basePath);
            updated = true;
        }

        for (String key : sourceSection.getKeys(false)) {
            String fullKey = basePath.isEmpty() ? key : basePath + "." + key;

            if (sourceSection.isConfigurationSection(key)) {
                updated |= mergeMissing(sourceRoot, targetRoot, fullKey);
                continue;
            }

            if (!targetRoot.contains(fullKey)) {
                targetRoot.set(fullKey, sourceRoot.get(fullKey));
                updated = true;
            }
        }

        return updated;
    }

    private void backup(File langFile) throws IOException {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(langFile.getParentFile(),
            langFile.getName().replace(".yml", "") + "_backup_" + timestamp + ".yml");
        Files.copy(langFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}


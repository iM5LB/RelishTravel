package relish.relishTravel.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    
    private final RelishTravel plugin;
    private final ConfigManager configManager;
    private final MiniMessage miniMessage;
    private FileConfiguration messages;
    private final Map<String, String> messageCache;
    
    public MessageManager(RelishTravel plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.miniMessage = MiniMessage.miniMessage();
        this.messageCache = new HashMap<>();
    }
    
    public void loadMessages() {
        messageCache.clear();
        String language = configManager.getLanguage();

        // Ensure language file exists and merge new keys from jar defaults (without overwriting custom values).
        File langFile;
        try {
            LangUpdater updater = new LangUpdater(plugin);
            langFile = updater.ensureAndUpdate(language);
        } catch (Throwable t) {
            // Never fail plugin enable due to language update errors.
            plugin.getLogger().warning("Language updater failed, falling back to en.yml: " + t.getMessage());
            langFile = new File(new File(plugin.getDataFolder(), "lang"), "en.yml");
        }

        // Load UTF-8 so Arabic and special characters are stable.
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(langFile.toPath()), StandardCharsets.UTF_8)) {
            this.messages = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load language file as UTF-8 (" + langFile.getName() + "): " + e.getMessage());
            this.messages = YamlConfiguration.loadConfiguration(langFile);
        }
        plugin.getLogger().info("Loaded language: " + language + " (" + langFile.getName() + ")");
    }
    
    public String getMessage(String path) {
        String fullPath = "messages." + path;
        
        if (messageCache.containsKey(fullPath)) {
            return messageCache.get(fullPath);
        }
        
        String message = messages.getString(fullPath, "");
        messageCache.put(fullPath, message);
        return message;
    }
    
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        
        return message;
    }
    
    public void sendMessage(Player player, String path) {
        String message = getMessage(path);
        if (!message.isEmpty()) {
            Component component = miniMessage.deserialize(message);
            player.sendMessage(component);
        }
    }
    
    public void sendMessage(Player player, String path, Map<String, String> placeholders) {
        String message = getMessage(path, placeholders);
        if (!message.isEmpty()) {
            Component component = miniMessage.deserialize(message);
            player.sendMessage(component);
        }
    }
    
    public void sendActionBar(Player player, String message) {
        Component component = miniMessage.deserialize(message);
        player.sendActionBar(component);
    }
    
    public Component parse(String message) {
        return miniMessage.deserialize(message);
    }
    
    public void sendConsoleMessage(String path) {
        String message = getMessage(path);
        if (!message.isEmpty()) {
            String plainMessage = message
                .replaceAll("<[^>]+>", "")
                .replaceAll("§[0-9a-fk-or]", "");
            plugin.getLogger().info(plainMessage);
        }
    }
    
    public void sendConsoleMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path, placeholders);
        if (!message.isEmpty()) {
            String plainMessage = message
                .replaceAll("<[^>]+>", "")
                .replaceAll("§[0-9a-fk-or]", "");
            plugin.getLogger().info(plainMessage);
        }
    }
}

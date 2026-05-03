package relish.relishTravel.util;

import org.bukkit.entity.Player;
import relish.relishTravel.RelishTravel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    
    private final RelishTravel plugin;
    private final String currentVersion;
    private final String pluginName;
    private final String website;
    private String latestVersion = null;
    private boolean updateAvailable = false;
    
    public UpdateChecker(RelishTravel plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.pluginName = plugin.getDescription().getName();
        this.website = plugin.getDescription().getWebsite();
    }
    
    public void checkForUpdates() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://api.github.com/repos/im5lb/RelishTravel/releases/latest");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                String json = response.toString();
                int tagStart = json.indexOf("\"tag_name\":\"") + 12;
                int tagEnd = json.indexOf("\"", tagStart);
                
                if (tagStart > 11 && tagEnd > tagStart) {
                    latestVersion = json.substring(tagStart, tagEnd).replace("v", "");
                    
                    if (!currentVersion.equals(latestVersion)) {
                        updateAvailable = true;
                        plugin.getLogger().info("Update available: v" + latestVersion + " (Current: v" + currentVersion + ")");
                        plugin.getLogger().info("Download: " + website);
                    }
                }
            } catch (Exception e) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("Could not check for updates: " + e.getMessage());
                }
            }
        });
    }
    
    public void notifyPlayer(Player player) {
        if (updateAvailable && player.hasPermission("relishtravel.admin")) {
            player.sendMessage("§8[§6" + pluginName + "§8] §eUpdate available: §av" + latestVersion + " §7(Current: v" + currentVersion + ") - §b" + website);
        }
    }
    
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    public String getLatestVersion() {
        return latestVersion;
    }
}

package relish.relishTravel.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;
import relish.relishTravel.message.MessageManager;

public class ReloadCommand implements CommandExecutor {
    
    private final RelishTravel plugin;
    private final ConfigManager config;
    private final MessageManager messages;
    
    public ReloadCommand(RelishTravel plugin, ConfigManager config, MessageManager messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("relishtravel.reload")) {
            if (sender instanceof Player player) {
                messages.sendMessage(player, "command.no-permission");
            } else {
                messages.sendConsoleMessage("command.no-permission");
            }
            return true;
        }
        
        try {
            plugin.reload();
            
            if (sender instanceof Player player) {
                messages.sendMessage(player, "command.reload-success");
            } else {
                messages.sendConsoleMessage("command.reload-success");
            }
        } catch (Exception e) {
            if (sender instanceof Player player) {
                messages.sendMessage(player, "command.reload-failed");
            } else {
                messages.sendConsoleMessage("command.reload-failed");
            }
            plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
}

package relish.relishTravel.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;
import relish.relishTravel.message.MessageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RelishTravelCommand implements CommandExecutor, TabCompleter {
    
    private final RelishTravel plugin;
    private final MessageManager messages;
    private final ReloadCommand reloadCommand;
    private final LaunchCommand launchCommand;
    private final CleanupCommand cleanupCommand;
    
    public RelishTravelCommand(RelishTravel plugin, ConfigManager config, MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.reloadCommand = new ReloadCommand(plugin, config, messages);
        this.launchCommand = new LaunchCommand(plugin, config, messages);
        this.cleanupCommand = new CleanupCommand(plugin, messages);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            return reloadCommand.onCommand(sender, command, label, 
                Arrays.copyOfRange(args, 1, args.length));
        }
        
        if (args.length > 0 && args[0].equalsIgnoreCase("launch")) {
            return launchCommand.onCommand(sender, command, label, 
                Arrays.copyOfRange(args, 1, args.length));
        }
        
        if (args.length > 0 && args[0].equalsIgnoreCase("cleanup")) {
            return cleanupCommand.onCommand(sender, command, label, 
                Arrays.copyOfRange(args, 1, args.length));
        }
        
        if (sender instanceof Player player) {
            messages.sendMessage(player, "command.help.header");
            messages.sendMessage(player, "command.help.reload");
            messages.sendMessage(player, "command.help.launch");
            messages.sendMessage(player, "command.help.cleanup");
            messages.sendMessage(player, "command.help.rtl");
            messages.sendMessage(player, "command.help.footer");
        } else {
            messages.sendConsoleMessage("command.help.console-usage");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            suggestions.add("reload");
            suggestions.add("launch");
            suggestions.add("cleanup");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("launch")) {
            suggestions.add("25");
            suggestions.add("50");
            suggestions.add("75");
            suggestions.add("100");
        }
        
        return suggestions;
    }
}

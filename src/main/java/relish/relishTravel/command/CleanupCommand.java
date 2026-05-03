package relish.relishTravel.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.message.MessageManager;

import java.util.ArrayList;
import java.util.List;

public class CleanupCommand implements CommandExecutor, TabCompleter {
    
    private final RelishTravel plugin;
    private final MessageManager messages;
    
    public CleanupCommand(RelishTravel plugin, MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length > 0) {
                if (!sender.hasPermission("relishtravel.admin")) {
                    messages.sendMessage(player, "command.no-permission");
                    return true;
                }
                
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage("§cPlayer not found: " + args[0]);
                    return true;
                }
                
                cleanupPlayer(target);
                player.sendMessage("§aCleanup completed for " + target.getName());
            } else {
                cleanupPlayer(player);
                messages.sendMessage(player, "command.cleanup-success");
            }
        } else {
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /relishtravel cleanup <player>");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[0]);
                return true;
            }
            
            cleanupPlayer(target);
            sender.sendMessage("§aCleanup completed for " + target.getName());
        }
        
        return true;
    }
    
    private void cleanupPlayer(Player player) {
        plugin.getElytraHandler().restoreChestplate(player);
        plugin.getLaunchHandler().handleGlideEnd(player);
        plugin.getChargeManager().cancelCharge(player);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        }
        
        return suggestions;
    }
}

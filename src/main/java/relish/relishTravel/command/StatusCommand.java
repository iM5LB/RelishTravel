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
import java.util.Map;

public class StatusCommand implements CommandExecutor, TabCompleter {

    private final RelishTravel plugin;
    private final MessageManager messages;

    public StatusCommand(RelishTravel plugin, MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /rt status <player>  →  admin checks another player
        if (args.length > 0) {
            if (!sender.hasPermission("relishtravel.toggle.others")) {
                if (sender instanceof Player p) messages.sendMessage(p, "command.no-permission");
                else sender.sendMessage("You don't have permission.");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                if (sender instanceof Player p) messages.sendMessage(p, "command.toggle.player-not-found");
                else sender.sendMessage("Player not found: " + args[0]);
                return true;
            }

            boolean enabled = plugin.isChargingEnabled(target);
            if (sender instanceof Player p) {
                messages.sendMessage(p,
                        enabled ? "command.toggle.status.player-enabled" : "command.toggle.status.player-disabled",
                        Map.of("player", target.getName()));
            } else {
                sender.sendMessage(target.getName() + " charging: " + (enabled ? "enabled" : "disabled"));
            }
            return true;
        }

        // /rt status  →  own status
        if (!(sender instanceof Player player)) {
            messages.sendConsoleMessage("command.help.console-usage");
            return true;
        }
        boolean enabled = plugin.isChargingEnabled(player);
        messages.sendMessage(player,
                enabled ? "command.toggle.status.enabled" : "command.toggle.status.disabled");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("relishtravel.toggle.others")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return names;
        }
        return List.of();
    }
}

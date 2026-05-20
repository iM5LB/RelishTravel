package relish.relishTravel.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.message.MessageManager;

import java.util.Arrays;
import java.util.List;

public class ToggleCommand implements CommandExecutor, TabCompleter {

    private final RelishTravel plugin;
    private final MessageManager messages;

    public ToggleCommand(RelishTravel plugin, MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.sendConsoleMessage("command.help.console-usage");
            return true;
        }

        // /rt toggle          → toggle charging
        // /rt toggle status   → show current charging state
        if (args.length > 0 && args[0].equalsIgnoreCase("status")) {
            boolean enabled = plugin.isChargingEnabled(player);
            if (enabled) {
                messages.sendMessage(player, "command.toggle.status.enabled");
            } else {
                messages.sendMessage(player, "command.toggle.status.disabled");
            }
            return true;
        }

        boolean enabled = plugin.toggleCharging(player);
        if (enabled) {
            messages.sendMessage(player, "command.toggle.enabled");
        } else {
            messages.sendMessage(player, "command.toggle.disabled");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("status");
        }
        return List.of();
    }
}

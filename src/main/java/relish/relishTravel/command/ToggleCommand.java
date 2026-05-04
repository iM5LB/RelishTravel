package relish.relishTravel.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.message.MessageManager;

public class ToggleCommand implements CommandExecutor {

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

        boolean enabled = plugin.toggleCharging(player);
        if (enabled) {
            messages.sendMessage(player, "command.toggle.enabled");
        } else {
            messages.sendMessage(player, "command.toggle.disabled");
        }
        return true;
    }
}


package Periklis20M.opLogin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class ResetOpCommand implements CommandExecutor {

    private final PasswordManager passwordManager;

    public ResetOpCommand(PasswordManager passwordManager) {
        this.passwordManager = passwordManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            sender.sendMessage(passwordManager.getMessage("messages.console-only"));
            return false;
        }

        if (args.length != 1) {
            sender.sendMessage("Usage: /resetop <player>");
            return false;
        }

        String playerName = args[0];
        if (!passwordManager.resetPassword(playerName)) {
            sender.sendMessage("Failed to reset password for " + playerName);
            return false;
        }

        sender.sendMessage(passwordManager.getPrefixedMessage(
            "messages.password-reset",
            Map.of("%player%", playerName)
        ));
        return true;
    }
}

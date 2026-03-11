package Periklis20M.opLogin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpSetPassCommand implements CommandExecutor {

    private final PasswordManager passwordManager;

    public OpSetPassCommand(PasswordManager passwordManager) {
        this.passwordManager = passwordManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            sender.sendMessage(passwordManager.getMessage("messages.console-only"));
            return false;
        }

        if (args.length != 2) {
            sender.sendMessage("Usage: /opsetpass <player> <password>");
            return false;
        }

        String playerName = args[0];
        String password = args[1];
        if (!passwordManager.setPassword(playerName, password)) {
            sender.sendMessage(passwordManager.getCredentialValidationMessage(password));
            return false;
        }

        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null && onlinePlayer.isOp()) {
            passwordManager.lockPlayer(onlinePlayer);
        }

        sender.sendMessage(passwordManager.getPrefixedMessage("messages.password-set", java.util.Map.of("%player%", playerName)));
        return true;
    }
}

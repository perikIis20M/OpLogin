package Periklis20M.opLogin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetOpCommand implements CommandExecutor {

    private final PasswordManager passwordManager;

    public ResetOpCommand(PasswordManager passwordManager) {
        this.passwordManager = passwordManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            sender.sendMessage("§cThis command can only be executed from the console.");
            return false;
        }

        if (args.length != 1) {
            sender.sendMessage("Usage: /resetop <player>");
            return false;
        }

        String player = args[0];

        if (passwordManager.resetPassword(player)) {
            sender.sendMessage("Password reset for " + player);
            return true;
        }

        sender.sendMessage("Failed to reset password for " + player);
        return false;
    }
} 
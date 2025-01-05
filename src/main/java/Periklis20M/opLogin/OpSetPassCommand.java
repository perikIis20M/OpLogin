package Periklis20M.opLogin;

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
            sender.sendMessage("§cThis command can only be executed from the console.");
            return false;
        }

        if (args.length != 2) {
            sender.sendMessage("Usage: /opsetpass <player> <password>");
            return false;
        }

        String player = args[0];
        String password = args[1];

        if (passwordManager.setPassword(player, password)) {
            sender.sendMessage("Password set for " + player);
            return true;
        }

        sender.sendMessage("Failed to set password for " + player);
        return false;
    }
} 
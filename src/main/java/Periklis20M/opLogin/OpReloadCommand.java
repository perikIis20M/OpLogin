package Periklis20M.opLogin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpReloadCommand implements CommandExecutor {

    private final PasswordManager passwordManager;

    public OpReloadCommand(PasswordManager passwordManager) {
        this.passwordManager = passwordManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            sender.sendMessage("§cThis command can only be executed from the console.");
            return false;
        }

        passwordManager.reloadAll();
        sender.sendMessage("§aOpLogin configuration and whitelist reloaded successfully!");
        return true;
    }
} 
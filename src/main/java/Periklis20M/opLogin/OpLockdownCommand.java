package Periklis20M.opLogin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpLockdownCommand implements CommandExecutor {

    private final PasswordManager passwordManager;

    public OpLockdownCommand(PasswordManager passwordManager) {
        this.passwordManager = passwordManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            sender.sendMessage(passwordManager.getMessage("messages.console-only"));
            return false;
        }

        passwordManager.forceLockAllOnlineOperators();
        sender.sendMessage(passwordManager.getPrefixedMessage("messages.lockdown-executed"));
        return true;
    }
}

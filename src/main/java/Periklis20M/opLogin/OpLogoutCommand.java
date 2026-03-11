package Periklis20M.opLogin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class OpLogoutCommand implements CommandExecutor {

    private final PasswordManager passwordManager;

    public OpLogoutCommand(PasswordManager passwordManager) {
        this.passwordManager = passwordManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return passwordManager.handleLogoutCommand(sender);
    }
}

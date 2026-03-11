package Periklis20M.opLogin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class OpReloadCommand implements CommandExecutor {

    private final PasswordManager passwordManager;
    private final JavaPlugin plugin;

    public OpReloadCommand(JavaPlugin plugin, PasswordManager passwordManager) {
        this.plugin = plugin;
        this.passwordManager = passwordManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            sender.sendMessage(passwordManager.getMessage("messages.console-only"));
            return false;
        }

        try {
            ((OpLogin) plugin).performReload();
            sender.sendMessage(passwordManager.getPrefixedMessage("messages.config-reloaded"));
            return true;
        } catch (Exception e) {
            sender.sendMessage("Error reloading OpLogin: " + e.getMessage());
            plugin.getLogger().severe("Error reloading OpLogin: " + e.getMessage());
            return false;
        }
    }
}

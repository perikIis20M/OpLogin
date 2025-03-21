package Periklis20M.opLogin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.ChatColor;

public class OpLoginCommand implements CommandExecutor {

    private final PasswordManager passwordManager;
    private final FileConfiguration config;

    public OpLoginCommand(PasswordManager passwordManager, FileConfiguration config) {
        this.passwordManager = passwordManager;
        this.config = config;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize(config.getString("messages.player-only")));
            return false;
        }

        Player player = (Player) sender;
        
        if (!player.isOp()) {
            player.sendMessage(colorize(config.getString("messages.op-only")));
            return false;
        }

        if (args.length != 1) {
            player.sendMessage(colorize(config.getString("messages.prefix") + "§cUsage: /oplogin <password>"));
            return false;
        }

        if (passwordManager.isLoggedIn(player.getName())) {
            player.sendMessage(colorize(config.getString("messages.prefix") + config.getString("messages.already-logged")));
            return false;
        }

        String playerName = player.getName();
        
        if (passwordManager.isPlayerBlocked(playerName)) {
            long minutes = passwordManager.getBlockTimeRemaining(playerName);
            String message = config.getString("messages.too-many-attempts")
                .replace("%minutes%", String.valueOf(minutes));
            player.sendMessage(colorize(config.getString("messages.prefix") + message));
            return false;
        }

        if (!passwordManager.hasPassword(playerName)) {
            player.sendMessage(colorize(config.getString("messages.prefix") + config.getString("messages.no-password")));
            return false;
        }

        // Hide password from console by not logging it
        if (passwordManager.checkPassword(playerName, args[0])) {
            passwordManager.loginPlayer(playerName);
            player.sendMessage(colorize(config.getString("messages.prefix") + config.getString("messages.login-success")));
            return true;
        }

        player.sendMessage(colorize(config.getString("messages.prefix") + config.getString("messages.login-failed")));
        return false;
    }
} 
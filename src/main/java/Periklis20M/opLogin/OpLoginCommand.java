package Periklis20M.opLogin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpLoginCommand implements CommandExecutor {

    private final PasswordManager passwordManager;

    public OpLoginCommand(PasswordManager passwordManager) {
        this.passwordManager = passwordManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return false;
        }

        Player player = (Player) sender;
        
        if (!player.isOp()) {
            player.sendMessage("§cThis command is only for server operators.");
            return false;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /oplogin <password>");
            return false;
        }

        if (passwordManager.isLoggedIn(player.getName())) {
            player.sendMessage("§cYou are already logged in!");
            return false;
        }

        String password = args[0];

        if (!passwordManager.hasPassword(player.getName())) {
            player.sendMessage("§cYou don't have a password set. Ask an administrator to set one for you.");
            return false;
        }

        if (passwordManager.checkPassword(player.getName(), password)) {
            passwordManager.loginPlayer(player.getName());
            player.sendMessage("§aLogin successful! You can now use OP commands.");
            return true;
        }

        player.sendMessage("§cIncorrect password.");
        return false;
    }
} 
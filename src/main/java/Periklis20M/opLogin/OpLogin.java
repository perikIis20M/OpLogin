package Periklis20M.opLogin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class OpLogin extends JavaPlugin implements Listener {

    private PasswordManager passwordManager;

    @Override
    public void onEnable() {
        // Initialize the password manager
        passwordManager = new PasswordManager(this);

        // Register commands
        this.getCommand("opsetpass").setExecutor(new OpSetPassCommand(passwordManager));
        this.getCommand("oplogin").setExecutor(new OpLoginCommand(passwordManager));
        this.getCommand("resetop").setExecutor(new ResetOpCommand(passwordManager));
        this.getCommand("opreload").setExecutor(new OpReloadCommand(passwordManager));

        // Register events
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Save passwords to storage
        if (passwordManager != null) {
            passwordManager.savePasswords();
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() && !passwordManager.isCommandWhitelisted(event.getMessage())) {
            if (!passwordManager.isLoggedIn(player.getName())) {
                event.setCancelled(true);
                player.sendMessage("§cYou must login first using /oplogin <password>");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        passwordManager.logoutPlayer(event.getPlayer().getName());
    }
}

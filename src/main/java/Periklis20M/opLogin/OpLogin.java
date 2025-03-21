package Periklis20M.opLogin;

import org.bukkit.ChatColor;
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
        this.getCommand("oplogin").setExecutor(new OpLoginCommand(passwordManager, getConfig()));
        this.getCommand("resetop").setExecutor(new ResetOpCommand(passwordManager));
        this.getCommand("opreload").setExecutor(new OpReloadCommand(this, passwordManager));

        // Register events
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Properly shutdown the password manager
        if (passwordManager != null) {
            passwordManager.shutdown();
            passwordManager = null;
        }
        
        // Force garbage collection to release file handles
        System.gc();
    }

    @Override
    public void reloadConfig() {
        // Just reload the plugin's config
        super.reloadConfig();
        saveDefaultConfig();
    }

    public void performReload() {
        try {
            // Reload the plugin's config
            reloadConfig();
            
            // Then reload the password manager
            if (passwordManager != null) {
                passwordManager.reloadAll();
            }
        } catch (Exception e) {
            getLogger().severe("Error during reload: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() && !passwordManager.isCommandWhitelisted(event.getMessage())) {
            if (!passwordManager.isLoggedIn(player.getName())) {
                event.setCancelled(true);
                String message = ChatColor.translateAlternateColorCodes('&', 
                    getConfig().getString("messages.prefix") + getConfig().getString("messages.must-login"));
                player.sendMessage(message);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        passwordManager.logoutPlayer(event.getPlayer().getName());
    }
}

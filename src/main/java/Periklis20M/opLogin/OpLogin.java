package Periklis20M.opLogin;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class OpLogin extends JavaPlugin implements Listener {

    private PasswordManager passwordManager;
    private PinLoginMenu pinLoginMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        passwordManager = new PasswordManager(this);
        pinLoginMenu = new PinLoginMenu(this, passwordManager);

        getCommand("opsetpass").setExecutor(new OpSetPassCommand(passwordManager));
        getCommand("oplogin").setExecutor(new OpLoginCommand(passwordManager));
        getCommand("oplogout").setExecutor(new OpLogoutCommand(passwordManager));
        getCommand("resetop").setExecutor(new ResetOpCommand(passwordManager));
        getCommand("opreload").setExecutor(new OpReloadCommand(this, passwordManager));
        getCommand("oplockdown").setExecutor(new OpLockdownCommand(passwordManager));

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(pinLoginMenu, this);
        passwordManager.refreshOnlineProtections();
    }

    @Override
    public void onDisable() {
        if (passwordManager != null) {
            passwordManager.shutdown();
            passwordManager = null;
        }
        if (pinLoginMenu != null) {
            pinLoginMenu.shutdown();
            pinLoginMenu = null;
        }
    }

    public void performReload() {
        reloadConfig();
        if (passwordManager != null) {
            passwordManager.reloadAll();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInternalCommand(PlayerCommandPreprocessEvent event) {
        if (!passwordManager.isInternalCommand(event.getMessage())) {
            return;
        }

        event.setCancelled(true);
        String[] parts = event.getMessage().trim().substring(1).split("\\s+");
        String label = parts[0].toLowerCase();
        if (label.contains(":")) {
            label = label.substring(label.indexOf(':') + 1);
        }
        String[] args = new String[Math.max(0, parts.length - 1)];
        if (parts.length > 1) {
            System.arraycopy(parts, 1, args, 0, parts.length - 1);
        }

        if ("oplogout".equals(label)) {
            passwordManager.handleLogoutCommand(event.getPlayer());
        } else {
            if (passwordManager.isPinModeEnabled()) {
                pinLoginMenu.beginPinLogin(event.getPlayer(), args);
            } else {
                passwordManager.beginLoginFlow(event.getPlayer(), args);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PasswordManager.JoinProtectionResult result = passwordManager.protectPlayerOnJoin(player);
        if (result == PasswordManager.JoinProtectionResult.AUTO_LOGGED_IN) {
            player.sendMessage(passwordManager.getPrefixedMessage("messages.auto-login"));
        } else if (result == PasswordManager.JoinProtectionResult.LOCKED) {
            player.sendMessage(passwordManager.getPrefixedMessage("messages.locked-on-join"));
            if (!passwordManager.hasPassword(player.getName())) {
                player.sendMessage(passwordManager.getPrefixedMessage("messages.no-password"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!passwordManager.isManagedOperator(player)) {
            return;
        }

        String rawCommand = event.getMessage();
        if (passwordManager.isInternalCommand(rawCommand)) {
            return;
        }

        if (passwordManager.isLocked(player)) {
            if (passwordManager.isSensitiveCommand(rawCommand)) {
                event.setCancelled(true);
                player.sendMessage(passwordManager.getPrefixedMessage("messages.sensitive-command-blocked"));
            } else if (!passwordManager.isCommandWhitelisted(rawCommand)) {
                event.setCancelled(true);
                player.sendMessage(passwordManager.getPrefixedMessage("messages.must-login"));
            }
            return;
        }

        passwordManager.auditCommand(player, rawCommand);
        if (passwordManager.isSensitiveCommand(rawCommand)) {
            passwordManager.scheduleSensitiveCommandAutoLock(player);
        }
        if (passwordManager.changesOperatorStatus(rawCommand)) {
            getServer().getScheduler().runTask(this, () -> passwordManager.handleOperatorStatusCommand(rawCommand));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        if (!passwordManager.isManagedOperator(player) || !passwordManager.isLocked(player)) {
            return;
        }

        event.getCommands().removeIf(command ->
            (!passwordManager.isCommandWhitelisted(command) && !passwordManager.isInternalCommandLabel(command))
                || passwordManager.isSensitiveCommand(command)
        );
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPasswordChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!passwordManager.isAwaitingPassword(player)) {
            return;
        }

        event.setCancelled(true);
        String password = PlainTextComponentSerializer.plainText().serialize(event.message());
        getServer().getScheduler().runTask(this, () -> passwordManager.handlePasswordInput(player, password));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (pinLoginMenu != null) {
            pinLoginMenu.clearSession(event.getPlayer());
        }
        passwordManager.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (passwordManager.changesOperatorStatus(event.getCommand())) {
            getServer().getScheduler().runTask(this, () -> passwordManager.handleOperatorStatusCommand(event.getCommand()));
        }
    }
}

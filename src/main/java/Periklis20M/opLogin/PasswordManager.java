package Periklis20M.opLogin;

import Periklis20M.opLogin.utils.EncryptionUtil;
import Periklis20M.opLogin.utils.PasswordHashUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class PasswordManager {

    private final JavaPlugin plugin;
    private final AuditLogger auditLogger;
    private final Map<String, String> passwords = new ConcurrentHashMap<>();
    private final Map<String, String> trustedIps = new ConcurrentHashMap<>();
    private final Set<String> protectedOperators = ConcurrentHashMap.newKeySet();
    private final Set<String> loggedInPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> lockedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> awaitingPasswordPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> whitelistedCommands = ConcurrentHashMap.newKeySet();
    private final Set<String> sensitiveCommands = ConcurrentHashMap.newKeySet();
    private final List<String> blockedPermissionPatterns = new ArrayList<>();
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, Instant> blockTimes = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> unlockTasks = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> postSensitiveLockTasks = new ConcurrentHashMap<>();
    private final Map<String, PermissionAttachment> lockAttachments = new ConcurrentHashMap<>();
    private EncryptionUtil encryptionUtil;
    private File configFile;
    private File whitelistFile;
    private FileConfiguration config;
    private FileConfiguration whitelist;

    public PasswordManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.auditLogger = new AuditLogger(plugin);
        setupFiles();
        ensureDefaultValues();
        initializeEncryption();
        enforceAuthenticationModeConsistency();
        loadAll();
    }

    private void setupFiles() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");
        whitelistFile = new File(plugin.getDataFolder(), "whitelist.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        if (!whitelistFile.exists()) {
            plugin.saveResource("whitelist.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        whitelist = YamlConfiguration.loadConfiguration(whitelistFile);
    }

    private void ensureDefaultValues() {
        boolean needsSave = false;

        if (!config.contains("security")) {
            config.createSection("security");
            needsSave = true;
        }
        if (!config.contains("protection")) {
            config.createSection("protection");
            needsSave = true;
        }
        if (!config.contains("messages")) {
            config.createSection("messages");
            needsSave = true;
        }

        needsSave |= setDefault("security.encryption-key", "");
        needsSave |= setDefault("security.min-password-length", 8);
        needsSave |= setDefault("security.max-login-attempts", 3);
        needsSave |= setDefault("security.block-duration", 15);
        needsSave |= setDefault("security.enable-ip-auto-login", true);
        needsSave |= setDefault("security.unlock-duration-seconds", 300);
        needsSave |= setDefault("security.log-failed-attempts", true);
        needsSave |= setDefault("security.auto-lock-ops-on-join", true);
        needsSave |= setDefault("security.pin-mode.enabled", false);
        needsSave |= setDefault("security.pin-mode.length", 4);
        needsSave |= setDefault("security.pin-mode.title", "&8OpLogin PIN");
        needsSave |= setDefault("security.auth-mode-marker", getConfiguredAuthMode());

        needsSave |= setDefault("protection.block-default-op-permissions", true);
        needsSave |= setDefault("protection.auto-logout-after-sensitive-command", true);
        needsSave |= setDefault("protection.blocked-permission-patterns", List.of(
            "*",
            "minecraft.command.*",
            "bukkit.command.*",
            "minecraft.admin.*",
            "paper.command.*",
            "minecraft.commandblock.*",
            "luckperms.*",
            "permissions.*",
            "pex.*"
        ));
        needsSave |= setDefault("protection.sensitive-commands", List.of(
            "op",
            "deop",
            "stop",
            "reload",
            "ban",
            "ban-ip",
            "pardon",
            "pardon-ip",
            "kick",
            "whitelist",
            "lp",
            "luckperms",
            "pex",
            "permissions",
            "minecraft:op",
            "minecraft:deop",
            "minecraft:stop",
            "minecraft:reload",
            "minecraft:ban",
            "minecraft:pardon",
            "bukkit:stop",
            "bukkit:reload"
        ));

        needsSave |= setDefault("messages.prefix", "&8[&bOpLogin&8] &7");
        needsSave |= setDefault("messages.login-success", "&aLogin successful! Operator privileges unlocked.");
        needsSave |= setDefault("messages.login-failed", "&cIncorrect password.");
        needsSave |= setDefault("messages.login-prompt", "&eType your password in chat. It will not be shown.");
        needsSave |= setDefault("messages.awaiting-password", "&ePassword entry already pending. Type it in chat.");
        needsSave |= setDefault("messages.command-password-disabled", "&cDo not put credentials in the command. Use /oplogin with no arguments.");
        needsSave |= setDefault("messages.logout-success", "&eOperator privileges locked.");
        needsSave |= setDefault("messages.pin-opened", "&ePIN keypad opened. Click the digits to unlock operator privileges.");
        needsSave |= setDefault("messages.pin-numeric-only", "&cPin mode is enabled. Credentials must contain only digits.");
        needsSave |= setDefault("messages.pin-length-invalid", "&cPin mode is enabled. Credentials must be exactly %length% digits long.");
        needsSave |= setDefault("messages.pin-closed", "&ePIN entry cancelled.");
        needsSave |= setDefault("messages.already-logged", "&cYou are already logged in.");
        needsSave |= setDefault("messages.already-locked", "&eYour operator privileges are already locked.");
        needsSave |= setDefault("messages.no-password", "&cYou do not have a password set. Ask an administrator to set one.");
        needsSave |= setDefault("messages.console-only", "&cThis command can only be executed from the console.");
        needsSave |= setDefault("messages.player-only", "&cThis command can only be used by players.");
        needsSave |= setDefault("messages.op-only", "&cThis command is only for server operators.");
        needsSave |= setDefault("messages.must-login", "&cOperator privileges are locked. Use /oplogin.");
        needsSave |= setDefault("messages.password-set", "&aPassword set for %player%.");
        needsSave |= setDefault("messages.password-reset", "&aPassword reset for %player%.");
        needsSave |= setDefault("messages.config-reloaded", "&aOpLogin configuration reloaded successfully.");
        needsSave |= setDefault("messages.too-short-password", "&cPassword must be at least %length% characters long.");
        needsSave |= setDefault("messages.too-many-attempts", "&cToo many failed attempts. Try again in %minutes% minutes.");
        needsSave |= setDefault("messages.auto-login", "&aAuto-login successful from your trusted IP.");
        needsSave |= setDefault("messages.locked-on-join", "&eYour operator privileges are locked until you authenticate.");
        needsSave |= setDefault("messages.auto-locked", "&eYour operator privileges have been locked again.");
        needsSave |= setDefault("messages.sensitive-command-blocked", "&cThat command is always blocked until you log in.");
        needsSave |= setDefault("messages.sensitive-command-logout", "&eSensitive command used. Operator privileges locked again.");
        needsSave |= setDefault("messages.lockdown-executed", "&eAll online operators have been returned to the locked state.");

        if (!config.contains("passwords")) {
            config.createSection("passwords");
            needsSave = true;
        }
        if (!config.contains("trusted-ips")) {
            config.createSection("trusted-ips");
            needsSave = true;
        }

        if (needsSave) {
            saveConfigFile();
        }
    }

    private boolean setDefault(String path, Object value) {
        if (!config.contains(path)) {
            config.set(path, value);
            return true;
        }
        return false;
    }

    private void initializeEncryption() {
        String existingKey = config.getString("security.encryption-key", "");
        encryptionUtil = (existingKey == null || existingKey.isBlank()) ? null : new EncryptionUtil(existingKey);
    }

    private void enforceAuthenticationModeConsistency() {
        String currentMode = getConfiguredAuthMode();
        String previousMode = config.getString("security.auth-mode-marker", currentMode);

        if (currentMode.equalsIgnoreCase(previousMode)) {
            return;
        }

        plugin.getLogger().warning(
            "Authentication mode changed from " + previousMode + " to " + currentMode
                + ". Resetting all stored operator credentials and trusted IPs."
        );

        passwords.clear();
        trustedIps.clear();
        loggedInPlayers.clear();
        awaitingPasswordPlayers.clear();
        failedAttempts.clear();
        blockTimes.clear();

        config.set("passwords", null);
        config.set("trusted-ips", null);
        config.set("security.auth-mode-marker", currentMode);
        saveConfigFile();
    }

    private String getConfiguredAuthMode() {
        return isPinModeEnabled() ? "pin" : "password";
    }

    private void loadAll() {
        loadPasswords();
        loadWhitelist();
        loadTrustedIps();
        loadSensitiveCommands();
        loadBlockedPermissionPatterns();
    }

    public void loadPasswords() {
        passwords.clear();
        ConfigurationSection section = config.getConfigurationSection("passwords");
        if (section == null) {
            return;
        }

        for (String player : section.getKeys(false)) {
            String encrypted = section.getString(player);
            if (encrypted != null && !encrypted.isEmpty()) {
                passwords.put(normalizePlayer(player), encrypted);
            }
        }
    }

    public void loadWhitelist() {
        whitelistedCommands.clear();
        for (String command : whitelist.getStringList("whitelisted-commands")) {
            whitelistedCommands.add(normalizeCommandKey(command));
        }
    }

    private void loadSensitiveCommands() {
        sensitiveCommands.clear();
        for (String command : config.getStringList("protection.sensitive-commands")) {
            sensitiveCommands.add(normalizeCommandKey(command));
        }
    }

    private void loadBlockedPermissionPatterns() {
        blockedPermissionPatterns.clear();
        for (String pattern : config.getStringList("protection.blocked-permission-patterns")) {
            String normalized = normalizePermission(pattern);
            if (!normalized.isEmpty()) {
                blockedPermissionPatterns.add(normalized);
            }
        }
    }

    private void loadTrustedIps() {
        trustedIps.clear();
        ConfigurationSection section = config.getConfigurationSection("trusted-ips");
        if (section == null) {
            return;
        }

        for (String player : section.getKeys(false)) {
            String ip = section.getString(player);
            if (ip != null && !ip.isEmpty()) {
                trustedIps.put(normalizePlayer(player), ip);
            }
        }
    }

    public boolean beginLoginFlow(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getMessage("messages.player-only"));
            return false;
        }

        if (!prepareLoginAttempt(player, args)) {
            return false;
        }

        String playerName = normalizePlayer(player.getName());
        if (!awaitingPasswordPlayers.add(playerName)) {
            player.sendMessage(getPrefixedMessage("messages.awaiting-password"));
            return false;
        }

        player.sendMessage(getPrefixedMessage("messages.login-prompt"));
        return true;
    }

    public boolean prepareLoginAttempt(Player player, String[] args) {
        if (!isManagedOperator(player)) {
            player.sendMessage(getMessage("messages.op-only"));
            return false;
        }

        if (args.length > 0) {
            player.sendMessage(getPrefixedMessage("messages.command-password-disabled"));
            return false;
        }

        String playerName = normalizePlayer(player.getName());
        if (isLoggedIn(playerName)) {
            player.sendMessage(getPrefixedMessage("messages.already-logged"));
            return false;
        }

        if (isPlayerBlocked(playerName)) {
            long minutes = Math.max(1, getBlockTimeRemaining(playerName));
            player.sendMessage(getPrefixedMessage(
                "messages.too-many-attempts",
                Map.of("%minutes%", String.valueOf(minutes))
            ));
            return false;
        }

        if (!hasPassword(playerName)) {
            player.sendMessage(getPrefixedMessage("messages.no-password"));
            return false;
        }
        return true;
    }

    public boolean handlePasswordInput(Player player, String password) {
        String playerName = normalizePlayer(player.getName());
        awaitingPasswordPlayers.remove(playerName);

        if (!isManagedOperator(player)) {
            player.sendMessage(getMessage("messages.op-only"));
            return false;
        }

        if (isLoggedIn(playerName)) {
            player.sendMessage(getPrefixedMessage("messages.already-logged"));
            return false;
        }

        if (isPlayerBlocked(playerName)) {
            long minutes = Math.max(1, getBlockTimeRemaining(playerName));
            player.sendMessage(getPrefixedMessage(
                "messages.too-many-attempts",
                Map.of("%minutes%", String.valueOf(minutes))
            ));
            return false;
        }

        if (!hasPassword(playerName)) {
            player.sendMessage(getPrefixedMessage("messages.no-password"));
            return false;
        }

        if (!checkPassword(playerName, password, player)) {
            player.sendMessage(getPrefixedMessage("messages.login-failed"));
            return false;
        }

        loginPlayer(player, true);
        player.sendMessage(getPrefixedMessage("messages.login-success"));
        return true;
    }

    public boolean handleLogoutCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getMessage("messages.player-only"));
            return false;
        }

        if (!isManagedOperator(player)) {
            player.sendMessage(getMessage("messages.op-only"));
            return false;
        }

        cancelPasswordPrompt(player);
        if (isLocked(player)) {
            player.sendMessage(getPrefixedMessage("messages.already-locked"));
            return false;
        }

        removeTrustedIp(player.getName());
        lockPlayer(player);
        player.sendMessage(getPrefixedMessage("messages.logout-success"));
        return true;
    }

    public void cancelPasswordPrompt(Player player) {
        awaitingPasswordPlayers.remove(normalizePlayer(player.getName()));
    }

    public boolean isAwaitingPassword(Player player) {
        return awaitingPasswordPlayers.contains(normalizePlayer(player.getName()));
    }

    public boolean isInternalCommand(String rawCommand) {
        ParsedCommand parsed = parseCommand(rawCommand);
        if (!parsed.isValid()) {
            return false;
        }
        return "oplogin".equals(parsed.shortLabel()) || "oplogout".equals(parsed.shortLabel());
    }

    public boolean isInternalCommandLabel(String commandLabel) {
        String normalized = normalizeCommandKey(commandLabel);
        if (normalized.contains(":")) {
            normalized = normalized.substring(normalized.indexOf(':') + 1);
        }
        return "oplogin".equals(normalized) || "oplogout".equals(normalized);
    }

    public JoinProtectionResult protectPlayerOnJoin(Player player) {
        if (!player.isOp() || !config.getBoolean("security.auto-lock-ops-on-join", true)) {
            clearSession(player, false);
            return JoinProtectionResult.NOT_PROTECTED;
        }

        String playerName = normalizePlayer(player.getName());
        protectedOperators.add(playerName);
        String playerIp = getPlayerIp(player);

        if (playerIp != null && isIpTrusted(playerName, playerIp)) {
            loginPlayer(player, false);
            return JoinProtectionResult.AUTO_LOGGED_IN;
        }

        lockPlayer(player);
        return JoinProtectionResult.LOCKED;
    }

    public void loginPlayer(Player player, boolean rememberIp) {
        String playerName = normalizePlayer(player.getName());
        protectedOperators.add(playerName);
        loggedInPlayers.add(playerName);
        lockedPlayers.remove(playerName);
        awaitingPasswordPlayers.remove(playerName);
        removeLockAttachment(player);
        cancelTask(unlockTasks.remove(playerName));
        cancelTask(postSensitiveLockTasks.remove(playerName));
        if (!player.isOp()) {
            player.setOp(true);
        }

        if (rememberIp) {
            String playerIp = getPlayerIp(player);
            if (playerIp != null && config.getBoolean("security.enable-ip-auto-login", true)) {
                setTrustedIp(playerName, playerIp);
            }
        }

        scheduleUnlockTimeout(player);
    }

    public void lockPlayer(Player player) {
        String playerName = normalizePlayer(player.getName());
        protectedOperators.add(playerName);
        loggedInPlayers.remove(playerName);
        awaitingPasswordPlayers.remove(playerName);
        lockedPlayers.add(playerName);
        cancelTask(unlockTasks.remove(playerName));
        cancelTask(postSensitiveLockTasks.remove(playerName));
        if (player.isOp()) {
            player.setOp(false);
        }
        applyLockAttachment(player);
    }

    public void forceLockAllOnlineOperators() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isManagedOperator(player) || player.isOp()) {
                lockPlayer(player);
            }
        }
    }

    public void handlePlayerQuit(Player player) {
        clearSession(player, true);
    }

    public void clearSession(Player player) {
        clearSession(player, false);
    }

    private void clearSession(Player player, boolean restoreOpStatus) {
        String playerName = normalizePlayer(player.getName());
        loggedInPlayers.remove(playerName);
        lockedPlayers.remove(playerName);
        awaitingPasswordPlayers.remove(playerName);
        cancelTask(unlockTasks.remove(playerName));
        cancelTask(postSensitiveLockTasks.remove(playerName));
        removeLockAttachment(player);
        if (restoreOpStatus && protectedOperators.remove(playerName)) {
            restoreOperatorStatus(player);
        } else if (!restoreOpStatus) {
            protectedOperators.remove(playerName);
        }
    }

    public boolean isManagedOperator(Player player) {
        String playerName = normalizePlayer(player.getName());
        return player.isOp()
            || protectedOperators.contains(playerName)
            || lockedPlayers.contains(playerName)
            || loggedInPlayers.contains(playerName);
    }

    public boolean isLocked(Player player) {
        return lockedPlayers.contains(normalizePlayer(player.getName()));
    }

    public boolean isLoggedIn(String player) {
        return loggedInPlayers.contains(normalizePlayer(player));
    }

    public boolean hasPassword(String player) {
        return passwords.containsKey(normalizePlayer(player));
    }

    public boolean setPassword(String player, String password) {
        if (!isCredentialValid(password)) {
            return false;
        }

        passwords.put(normalizePlayer(player), PasswordHashUtil.hash(password));
        savePasswords();
        return true;
    }

    public boolean resetPassword(String player) {
        String playerName = normalizePlayer(player);
        boolean removed = passwords.remove(playerName) != null;
        if (!removed) {
            return false;
        }

        trustedIps.remove(playerName);
        savePasswords();
        saveTrustedIps();

        Player onlinePlayer = Bukkit.getPlayerExact(player);
        if (onlinePlayer != null) {
            lockPlayer(onlinePlayer);
        } else {
            loggedInPlayers.remove(playerName);
            lockedPlayers.remove(playerName);
            protectedOperators.remove(playerName);
            awaitingPasswordPlayers.remove(playerName);
        }

        return true;
    }

    public boolean isCommandWhitelisted(String rawCommand) {
        ParsedCommand parsed = parseCommand(rawCommand);
        return parsed.isValid() && whitelistedCommands.contains(parsed.shortLabel()) && !isSensitiveCommand(rawCommand);
    }

    public boolean isSensitiveCommand(String rawCommand) {
        ParsedCommand parsed = parseCommand(rawCommand);
        return parsed.isValid() && (
            sensitiveCommands.contains(parsed.label()) || sensitiveCommands.contains(parsed.shortLabel())
        );
    }

    public void auditCommand(Player player, String rawCommand) {
        if (!isManagedOperator(player) || !isLoggedIn(player.getName())) {
            return;
        }
        if (isCommandWhitelisted(rawCommand) && !isSensitiveCommand(rawCommand)) {
            return;
        }
        if (isInternalCommand(rawCommand)) {
            return;
        }
        auditLogger.logCommand(player.getName(), rawCommand);
    }

    public void scheduleSensitiveCommandAutoLock(Player player) {
        if (!config.getBoolean("protection.auto-logout-after-sensitive-command", true)) {
            return;
        }

        String playerName = normalizePlayer(player.getName());
        cancelTask(postSensitiveLockTasks.remove(playerName));
        BukkitTask task = Bukkit.getScheduler().runTask(plugin, () -> {
            Player livePlayer = Bukkit.getPlayerExact(player.getName());
            if (livePlayer == null || !livePlayer.isOnline() || !isLoggedIn(playerName)) {
                return;
            }
            lockPlayer(livePlayer);
            livePlayer.sendMessage(getPrefixedMessage("messages.sensitive-command-logout"));
        });
        postSensitiveLockTasks.put(playerName, task);
    }

    public boolean changesOperatorStatus(String rawCommand) {
        ParsedCommand parsed = parseCommand(rawCommand);
        if (!parsed.isValid()) {
            return false;
        }
        return "op".equals(parsed.shortLabel()) || "deop".equals(parsed.shortLabel());
    }

    public void handleOperatorStatusCommand(String rawCommand) {
        String[] parts = tokenizeCommand(rawCommand);
        if (parts.length < 2) {
            return;
        }

        ParsedCommand parsed = parseCommand(parts[0]);
        if (!parsed.isValid()) {
            return;
        }

        String targetName = parts[1];
        String normalizedTarget = normalizePlayer(targetName);

        if ("deop".equals(parsed.shortLabel())) {
            Player onlinePlayer = Bukkit.getPlayerExact(targetName);
            if (onlinePlayer != null) {
                clearSession(onlinePlayer, false);
            } else {
                protectedOperators.remove(normalizedTarget);
                loggedInPlayers.remove(normalizedTarget);
                lockedPlayers.remove(normalizedTarget);
                awaitingPasswordPlayers.remove(normalizedTarget);
                cancelTask(unlockTasks.remove(normalizedTarget));
                cancelTask(postSensitiveLockTasks.remove(normalizedTarget));
            }
            return;
        }

        if ("op".equals(parsed.shortLabel())) {
            Player onlinePlayer = Bukkit.getPlayerExact(targetName);
            if (onlinePlayer != null) {
                protectedOperators.add(normalizedTarget);
                lockPlayer(onlinePlayer);
            }
        }
    }

    public void refreshOnlineProtections() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerName = normalizePlayer(player.getName());
            boolean managed = protectedOperators.contains(playerName);
            if (!player.isOp() && !managed) {
                clearSession(player, false);
                continue;
            }

            if (isLoggedIn(playerName)) {
                loginPlayer(player, false);
            } else {
                lockPlayer(player);
            }
        }
    }

    public void reloadAll() {
        config = YamlConfiguration.loadConfiguration(configFile);
        whitelist = YamlConfiguration.loadConfiguration(whitelistFile);
        ensureDefaultValues();
        initializeEncryption();
        enforceAuthenticationModeConsistency();
        loadAll();
        refreshOnlineProtections();
    }

    public void shutdown() {
        savePasswords();
        saveTrustedIps();
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearSession(player, true);
        }
        protectedOperators.clear();
        awaitingPasswordPlayers.clear();
        passwords.clear();
        trustedIps.clear();
        whitelistedCommands.clear();
        sensitiveCommands.clear();
        blockedPermissionPatterns.clear();
        failedAttempts.clear();
        blockTimes.clear();
    }

    public boolean isPlayerBlocked(String player) {
        String playerName = normalizePlayer(player);
        Instant blockUntil = blockTimes.get(playerName);
        if (blockUntil == null) {
            return false;
        }
        if (Instant.now().isBefore(blockUntil)) {
            return true;
        }

        blockTimes.remove(playerName);
        failedAttempts.remove(playerName);
        return false;
    }

    public long getBlockTimeRemaining(String player) {
        String playerName = normalizePlayer(player);
        Instant blockUntil = blockTimes.get(playerName);
        if (blockUntil == null) {
            return 0;
        }

        long seconds = blockUntil.getEpochSecond() - Instant.now().getEpochSecond();
        return seconds <= 0 ? 0 : (long) Math.ceil(seconds / 60.0);
    }

    public String getMessage(String path) {
        return colorize(config.getString(path, "&cMissing message: " + path));
    }

    public String getPrefixedMessage(String path) {
        return colorize(getPrefix() + config.getString(path, ""));
    }

    public String getPrefixedMessage(String path, Map<String, String> replacements) {
        String message = getPrefix() + config.getString(path, "");
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        return colorize(message);
    }

    public String getPrefix() {
        return config.getString("messages.prefix", "");
    }

    public int getMinPasswordLength() {
        return config.getInt("security.min-password-length", 8);
    }

    public boolean isPinModeEnabled() {
        return config.getBoolean("security.pin-mode.enabled", false);
    }

    public int getPinLength() {
        return config.getInt("security.pin-mode.length", 4);
    }

    public String getPinTitle() {
        return colorize(config.getString("security.pin-mode.title", "&8OpLogin PIN"));
    }

    public boolean isCredentialValid(String credential) {
        if (credential == null || credential.isEmpty()) {
            return false;
        }
        if (isPinModeEnabled()) {
            return credential.matches("\\d{" + getPinLength() + "}");
        }
        return credential.length() >= getMinPasswordLength();
    }

    public String getCredentialValidationMessage() {
        if (!isPinModeEnabled()) {
            return getPrefixedMessage(
                "messages.too-short-password",
                Map.of("%length%", String.valueOf(getMinPasswordLength()))
            );
        }
        return getPrefixedMessage("messages.pin-numeric-only");
    }

    public String getCredentialValidationMessage(String credential) {
        if (!isPinModeEnabled()) {
            return getPrefixedMessage(
                "messages.too-short-password",
                Map.of("%length%", String.valueOf(getMinPasswordLength()))
            );
        }
        if (credential == null || !credential.matches("\\d+")) {
            return getPrefixedMessage("messages.pin-numeric-only");
        }
        return getPrefixedMessage(
            "messages.pin-length-invalid",
            Map.of("%length%", String.valueOf(getPinLength()))
        );
    }

    private boolean checkPassword(String player, String password, Player sourcePlayer) {
        String storedPassword = passwords.get(normalizePlayer(player));
        if (storedPassword == null) {
            return false;
        }

        if (isPlayerBlocked(player)) {
            return false;
        }

        boolean matches;
        if (PasswordHashUtil.isHash(storedPassword)) {
            matches = PasswordHashUtil.verify(password, storedPassword);
        } else {
            matches = checkLegacyPassword(player, password, storedPassword);
        }

        if (!matches) {
            recordFailedAttempt(player, sourcePlayer);
        } else {
            resetFailedAttempts(player);
        }

        return matches;
    }

    private boolean checkLegacyPassword(String player, String password, String storedPassword) {
        if (encryptionUtil == null) {
            plugin.getLogger().warning("Legacy password exists for " + player + " but no legacy encryption key is configured.");
            return false;
        }

        String decrypted = encryptionUtil.decrypt(storedPassword);
        if (decrypted == null || !decrypted.equals(password)) {
            return false;
        }

        passwords.put(normalizePlayer(player), PasswordHashUtil.hash(password));
        savePasswords();
        plugin.getLogger().info("Migrated legacy encrypted password for " + player + " to BCrypt.");
        return true;
    }

    private void recordFailedAttempt(String player, Player sourcePlayer) {
        String playerName = normalizePlayer(player);
        int attempts = failedAttempts.getOrDefault(playerName, 0) + 1;
        failedAttempts.put(playerName, attempts);

        if (config.getBoolean("security.log-failed-attempts", true)) {
            String address = getPlayerIp(sourcePlayer);
            plugin.getLogger().warning(
                "Failed operator login attempt for " + sourcePlayer.getName()
                    + (address == null ? "" : " from " + address)
                    + " (" + attempts + "/" + config.getInt("security.max-login-attempts", 3) + ")"
            );
        }

        if (attempts >= config.getInt("security.max-login-attempts", 3)) {
            Instant blockedUntil = Instant.now().plusSeconds(config.getInt("security.block-duration", 15) * 60L);
            blockTimes.put(playerName, blockedUntil);
            plugin.getLogger().warning(
                "Operator login temporarily blocked for " + sourcePlayer.getName()
                    + " until " + blockedUntil + "."
            );
        }
    }

    private void resetFailedAttempts(String player) {
        String playerName = normalizePlayer(player);
        failedAttempts.remove(playerName);
        blockTimes.remove(playerName);
    }

    private void savePasswords() {
        config.set("passwords", null);
        for (Map.Entry<String, String> entry : passwords.entrySet()) {
            config.set("passwords." + entry.getKey(), entry.getValue());
        }
        saveConfigFile();
    }

    private void saveTrustedIps() {
        config.set("trusted-ips", null);
        for (Map.Entry<String, String> entry : trustedIps.entrySet()) {
            config.set("trusted-ips." + entry.getKey(), entry.getValue());
        }
        saveConfigFile();
    }

    public void setTrustedIp(String player, String ip) {
        trustedIps.put(normalizePlayer(player), ip);
        saveTrustedIps();
    }

    public void removeTrustedIp(String player) {
        if (trustedIps.remove(normalizePlayer(player)) != null) {
            saveTrustedIps();
        }
    }

    public boolean isIpTrusted(String player, String ip) {
        if (!config.getBoolean("security.enable-ip-auto-login", true)) {
            return false;
        }

        String storedIp = trustedIps.get(normalizePlayer(player));
        return storedIp != null && storedIp.equals(ip) && hasPassword(player);
    }

    private void scheduleUnlockTimeout(Player player) {
        String playerName = normalizePlayer(player.getName());
        cancelTask(unlockTasks.remove(playerName));

        int unlockDurationSeconds = config.getInt("security.unlock-duration-seconds", 300);
        if (unlockDurationSeconds <= 0) {
            return;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(
            plugin,
            () -> {
                Player livePlayer = Bukkit.getPlayerExact(player.getName());
                if (livePlayer == null || !livePlayer.isOnline() || !isLoggedIn(playerName)) {
                    return;
                }
                lockPlayer(livePlayer);
                livePlayer.sendMessage(getPrefixedMessage("messages.auto-locked"));
            },
            unlockDurationSeconds * 20L
        );
        unlockTasks.put(playerName, task);
    }

    private void applyLockAttachment(Player player) {
        removeLockAttachment(player);
        PermissionAttachment attachment = player.addAttachment(plugin);
        for (String permission : collectPermissionsToBlock(player)) {
            attachment.setPermission(permission, false);
        }
        lockAttachments.put(normalizePlayer(player.getName()), attachment);
        player.recalculatePermissions();
    }

    private void removeLockAttachment(Player player) {
        String playerName = normalizePlayer(player.getName());
        PermissionAttachment attachment = lockAttachments.remove(playerName);
        if (attachment != null) {
            player.removeAttachment(attachment);
            player.recalculatePermissions();
        }
    }

    private Set<String> collectPermissionsToBlock(Player player) {
        Set<String> permissionsToBlock = new HashSet<>();

        if (config.getBoolean("protection.block-default-op-permissions", true)) {
            Collection<Permission> permissions = plugin.getServer().getPluginManager().getPermissions();
            for (Permission permission : permissions) {
                if (permission.getDefault() == PermissionDefault.OP) {
                    permissionsToBlock.add(permission.getName().toLowerCase(Locale.ROOT));
                }
            }
        }

        for (Permission permission : plugin.getServer().getPluginManager().getPermissions()) {
            String name = permission.getName().toLowerCase(Locale.ROOT);
            if (matchesBlockedPattern(name)) {
                permissionsToBlock.add(name);
            }
        }

        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String name = info.getPermission().toLowerCase(Locale.ROOT);
            if (matchesBlockedPattern(name)) {
                permissionsToBlock.add(name);
            }
        }

        for (String pattern : blockedPermissionPatterns) {
            if (!pattern.contains("*")) {
                permissionsToBlock.add(pattern);
            }
        }

        return permissionsToBlock;
    }

    private boolean matchesBlockedPattern(String permission) {
        String normalizedPermission = normalizePermission(permission);
        for (String pattern : blockedPermissionPatterns) {
            if (globMatches(pattern, normalizedPermission)) {
                return true;
            }
        }
        return false;
    }

    private boolean globMatches(String pattern, String value) {
        if ("*".equals(pattern)) {
            return "*".equals(value);
        }
        String regex = "^" + Pattern.quote(pattern).replace("\\*", "\\E.*\\Q") + "$";
        return value.matches(regex);
    }

    private ParsedCommand parseCommand(String rawCommand) {
        String[] parts = tokenizeCommand(rawCommand);
        if (parts.length == 0 || parts[0].isEmpty()) {
            return ParsedCommand.invalid();
        }

        String label = normalizeCommandKey(parts[0]);
        String shortLabel = label.contains(":") ? label.substring(label.indexOf(':') + 1) : label;
        return new ParsedCommand(label, shortLabel);
    }

    private String[] tokenizeCommand(String rawCommand) {
        if (rawCommand == null) {
            return new String[0];
        }

        String trimmed = rawCommand.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.isEmpty()) {
            return new String[0];
        }

        return trimmed.split("\\s+");
    }

    private void saveConfigFile() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }

    private void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    private void restoreOperatorStatus(Player player) {
        if (player.isOp()) {
            return;
        }

        player.setOp(true);
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
        if (!offlinePlayer.isOp()) {
            offlinePlayer.setOp(true);
        }
    }

    private String getPlayerIp(Player player) {
        if (player == null) {
            return null;
        }

        InetSocketAddress address = player.getAddress();
        if (address == null || address.getAddress() == null) {
            return null;
        }
        return address.getAddress().getHostAddress();
    }

    private String normalizePlayer(String player) {
        return player == null ? "" : player.toLowerCase(Locale.ROOT);
    }

    private String normalizeCommandKey(String command) {
        return command == null ? "" : command.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePermission(String permission) {
        return permission == null ? "" : permission.trim().toLowerCase(Locale.ROOT);
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }

    public enum JoinProtectionResult {
        NOT_PROTECTED,
        LOCKED,
        AUTO_LOGGED_IN
    }

    private record ParsedCommand(String label, String shortLabel) {
        private static ParsedCommand invalid() {
            return new ParsedCommand("", "");
        }

        private boolean isValid() {
            return !label.isEmpty();
        }
    }
}

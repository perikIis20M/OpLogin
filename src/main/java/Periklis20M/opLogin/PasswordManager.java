package Periklis20M.opLogin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import Periklis20M.opLogin.utils.EncryptionUtil;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class PasswordManager {
    private final JavaPlugin plugin;
    private final Map<String, String> passwords = new HashMap<>();
    private final Set<String> loggedInPlayers = new HashSet<>();
    private final Set<String> whitelistedCommands = new HashSet<>();
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, Instant> blockTimes = new ConcurrentHashMap<>();
    private EncryptionUtil encryptionUtil;
    private File configFile;
    private File whitelistFile;
    private FileConfiguration config;
    private FileConfiguration whitelist;

    public PasswordManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupFiles();
        ensureDefaultValues();
        loadPasswords();
        loadWhitelist();
        initializeEncryption();
    }

    private void setupFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Setup config.yml
        configFile = new File(plugin.getDataFolder(), "config.yml");
        config = new YamlConfiguration();
        
        if (!configFile.exists()) {
            // Load defaults from jar
            try {
                // First save the resource
                plugin.saveResource("config.yml", false);
                // Then load it from the file
                config.load(configFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Could not create default config.yml");
            }
        } else {
            // Load existing config
            try {
                config.load(configFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Could not load config.yml");
            }
        }

        // Setup whitelist.yml
        whitelistFile = new File(plugin.getDataFolder(), "whitelist.yml");
        whitelist = new YamlConfiguration();
        
        if (!whitelistFile.exists()) {
            try {
                // First save the resource
                plugin.saveResource("whitelist.yml", false);
                // Then load it from the file
                whitelist.load(whitelistFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Could not create default whitelist.yml");
            }
        } else {
            try {
                whitelist.load(whitelistFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Could not load whitelist.yml");
            }
        }
    }

    private void ensureDefaultValues() {
        boolean needsSave = false;
        
        if (!config.contains("security")) {
            needsSave = true;
            config.createSection("security");
            config.set("security.min-password-length", 8);
            config.set("security.max-login-attempts", 3);
            config.set("security.block-duration", 15);
        }
        
        if (!config.contains("messages")) {
            needsSave = true;
            config.createSection("messages");
            config.set("messages.prefix", "&8[&bOpLogin&8] &7");
            config.set("messages.login-success", "&aLogin successful! You can now use OP commands.");
            config.set("messages.login-failed", "&cIncorrect password.");
            config.set("messages.already-logged", "&cYou are already logged in!");
            config.set("messages.no-password", "&cYou don't have a password set. Ask an administrator to set one for you.");
            config.set("messages.console-only", "&cThis command can only be executed from the console.");
            config.set("messages.player-only", "&cThis command can only be used by players.");
            config.set("messages.op-only", "&cThis command is only for server operators.");
            config.set("messages.must-login", "&cYou must login first using /oplogin <password>");
            config.set("messages.password-set", "&aPassword set for %player%");
            config.set("messages.password-reset", "&aPassword reset for %player%");
            config.set("messages.config-reloaded", "&aOpLogin configuration and whitelist reloaded successfully!");
            config.set("messages.too-short-password", "&cPassword must be at least %length% characters long.");
            config.set("messages.too-many-attempts", "&cToo many failed attempts. Please try again in %minutes% minutes.");
        }

        if (needsSave) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save default config values");
            }
        }
    }

    public void loadWhitelist() {
        whitelistedCommands.clear();
        List<String> commands = whitelist.getStringList("whitelisted-commands");
        whitelistedCommands.addAll(commands.stream().map(String::toLowerCase).toList());
    }

    public boolean isCommandWhitelisted(String command) {
        // Remove the '/' prefix if present and convert to lowercase
        String cleanCommand = command.startsWith("/") ? 
            command.substring(1).toLowerCase() : 
            command.toLowerCase();
            
        // Split the command to get the base command (e.g., "tp" from "tp player")
        String baseCommand = cleanCommand.split(" ")[0];
        
        return whitelistedCommands.contains(baseCommand);
    }

    private void initializeEncryption() {
        String existingKey = config.getString("security.encryption-key", "");
        encryptionUtil = new EncryptionUtil(existingKey);
        
        if (existingKey.isEmpty()) {
            config.set("security.encryption-key", encryptionUtil.getEncodedKey());
            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save encryption key!");
            }
        }
    }

    public void loadPasswords() {
        passwords.clear();
        if (config.contains("passwords")) {
            for (String player : config.getConfigurationSection("passwords").getKeys(false)) {
                String encrypted = config.getString("passwords." + player);
                passwords.put(player.toLowerCase(), encrypted);
            }
        }
    }

    public void savePasswords() {
        config.set("passwords", null);
        for (Map.Entry<String, String> entry : passwords.entrySet()) {
            config.set("passwords." + entry.getKey(), entry.getValue());
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save passwords to config.yml");
        }
    }

    public boolean setPassword(String player, String password) {
        if (password.length() < config.getInt("security.min-password-length", 8)) {
            return false;
        }
        String encrypted = encryptionUtil.encrypt(password);
        passwords.put(player.toLowerCase(), encrypted);
        savePasswords();
        return true;
    }

    public boolean checkPassword(String player, String password) {
        String playerKey = player.toLowerCase();
        String encrypted = passwords.get(playerKey);
        if (encrypted == null) return false;

        if (isPlayerBlocked(playerKey)) {
            return false;
        }

        boolean matches = encryptionUtil.decrypt(encrypted).equals(password);
        if (!matches) {
            recordFailedAttempt(playerKey);
        } else {
            resetFailedAttempts(playerKey);
        }
        return matches;
    }

    private void recordFailedAttempt(String player) {
        int attempts = failedAttempts.getOrDefault(player, 0) + 1;
        failedAttempts.put(player, attempts);
        
        if (attempts >= config.getInt("security.max-login-attempts", 3)) {
            blockTimes.put(player, Instant.now().plusSeconds(
                config.getInt("security.block-duration", 15) * 60L));
        }
    }

    private void resetFailedAttempts(String player) {
        failedAttempts.remove(player);
        blockTimes.remove(player);
    }

    public boolean isPlayerBlocked(String player) {
        Instant blockUntil = blockTimes.get(player);
        if (blockUntil != null) {
            if (Instant.now().isBefore(blockUntil)) {
                return true;
            } else {
                blockTimes.remove(player);
                failedAttempts.remove(player);
            }
        }
        return false;
    }

    public long getBlockTimeRemaining(String player) {
        Instant blockUntil = blockTimes.get(player);
        if (blockUntil != null && Instant.now().isBefore(blockUntil)) {
            return (blockUntil.getEpochSecond() - Instant.now().getEpochSecond()) / 60;
        }
        return 0;
    }

    public boolean resetPassword(String player) {
        boolean result = passwords.remove(player.toLowerCase()) != null;
        if (result) {
            savePasswords();
        }
        return result;
    }

    public void loginPlayer(String player) {
        loggedInPlayers.add(player.toLowerCase());
    }

    public void logoutPlayer(String player) {
        loggedInPlayers.remove(player.toLowerCase());
    }

    public boolean isLoggedIn(String player) {
        return loggedInPlayers.contains(player.toLowerCase());
    }

    public boolean hasPassword(String player) {
        return passwords.containsKey(player.toLowerCase());
    }

    public void shutdown() {
        // Save any pending changes
        savePasswords();
        
        // Clear all collections
        passwords.clear();
        loggedInPlayers.clear();
        whitelistedCommands.clear();
        failedAttempts.clear();
        blockTimes.clear();
        
        // Clear file references
        config = null;
        whitelist = null;
        configFile = null;
        whitelistFile = null;
        encryptionUtil = null;
    }

    private void backupAndRestoreConfig() {
        try {
            // Create backup files
            File configBackup = new File(plugin.getDataFolder(), "config.yml.backup");
            File whitelistBackup = new File(plugin.getDataFolder(), "whitelist.yml.backup");
            
            // Copy current files to backup if they exist
            if (configFile.exists()) {
                java.nio.file.Files.copy(
                    configFile.toPath(),
                    configBackup.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }
            
            if (whitelistFile.exists()) {
                java.nio.file.Files.copy(
                    whitelistFile.toPath(),
                    whitelistBackup.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }
            
            // Let the reload happen
            plugin.reloadConfig();
            
            // Restore from backups
            if (configBackup.exists()) {
                java.nio.file.Files.copy(
                    configBackup.toPath(),
                    configFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }
            
            if (whitelistBackup.exists()) {
                java.nio.file.Files.copy(
                    whitelistBackup.toPath(),
                    whitelistFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }
            
            // Delete backup files
            configBackup.delete();
            whitelistBackup.delete();
            
        } catch (IOException e) {
            plugin.getLogger().severe("Error during config backup/restore: " + e.getMessage());
        }
    }

    public void reloadAll() {
        try {
            // Just reload the existing files, DO NOT SAVE OR MODIFY THEM
            if (configFile != null && configFile.exists()) {
                config = YamlConfiguration.loadConfiguration(configFile);
            }
            
            if (whitelistFile != null && whitelistFile.exists()) {
                whitelist = YamlConfiguration.loadConfiguration(whitelistFile);
            }
            
            // Re-initialize with preserved data
            initializeEncryption();
            loadPasswords();
            loadWhitelist();
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error during reload: " + e.getMessage());
        }
    }
} 
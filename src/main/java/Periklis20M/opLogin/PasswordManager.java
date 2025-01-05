package Periklis20M.opLogin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PasswordManager {
    private final JavaPlugin plugin;
    private final Map<String, String> passwords = new HashMap<>();
    private final Set<String> loggedInPlayers = new HashSet<>();
    private final Set<String> whitelistedCommands = new HashSet<>();
    private File configFile;
    private File whitelistFile;
    private FileConfiguration config;
    private FileConfiguration whitelist;

    public PasswordManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupFiles();
        loadPasswords();
        loadWhitelist();
    }

    private void setupFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Setup config.yml
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Setup whitelist.yml
        whitelistFile = new File(plugin.getDataFolder(), "whitelist.yml");
        if (!whitelistFile.exists()) {
            plugin.saveResource("whitelist.yml", false);
        }
        whitelist = YamlConfiguration.loadConfiguration(whitelistFile);
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

    public void loadPasswords() {
        passwords.clear();
        if (config.contains("passwords")) {
            for (String player : config.getConfigurationSection("passwords").getKeys(false)) {
                passwords.put(player, config.getString("passwords." + player));
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
        passwords.put(player.toLowerCase(), password);
        savePasswords();
        return true;
    }

    public boolean checkPassword(String player, String password) {
        return passwords.getOrDefault(player.toLowerCase(), "").equals(password);
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

    public void reloadAll() {
        loadPasswords();
        loadWhitelist();
    }
} 
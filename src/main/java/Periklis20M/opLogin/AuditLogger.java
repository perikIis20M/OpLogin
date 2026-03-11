package Periklis20M.opLogin;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class AuditLogger {

    private final JavaPlugin plugin;
    private final File auditFile;

    public AuditLogger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.auditFile = new File(plugin.getDataFolder(), "operator-audit.log");
    }

    public synchronized void logCommand(String playerName, String command) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for audit logging.");
            return;
        }

        String line = "[" + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "] "
            + playerName + " executed: " + command + System.lineSeparator();

        try {
            Files.writeString(
                auditFile.toPath(),
                line,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            plugin.getLogger().warning("Could not write operator audit log: " + e.getMessage());
        }
    }
}

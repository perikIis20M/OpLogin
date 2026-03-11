package Periklis20M.opLogin;

import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PinLoginMenu implements Listener {

    private static final int INVENTORY_SIZE = 27;
    private static final int DISPLAY_SLOT = 4;
    private static final int CLEAR_SLOT = 18;
    private static final int BACKSPACE_SLOT = 24;
    private static final int CANCEL_SLOT = 26;
    private static final int[] DIGIT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 20, 21, 22};
    private static final String CREDIT_URL = "https://minecraft-heads.com/";
    private static final String CREDIT_LINE = "Textures: minecraft-heads.com";
    private static final Map<Integer, String> DIGIT_TEXTURES = Map.of(
        0, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWY4ODZkOWM0MGVmN2Y1MGMyMzg4MjQ3OTJjNDFmYmZiNTRmNjY1ZjE1OWJmMWJjYjBiMjdiM2VhZDM3M2IifX19",
        1, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTg3NDQ0ZWQ2ZDg0NTY5MDExNWIzODU4OGNmMWEyNjQ0ZGE5YzcxZTZmMzFhZTI0NjUxMDM4Y2IxZDQ3NmVhIn19fQ==",
        2, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWU0NGI3MDdiMjA1YjdkMjNlNDBhM2MwZGYxOWIwOWI2MmQwMjc4MDcyNzhlZjMwMWIzYzJhYjk4YjRmMGQ2In19fQ==",
        3, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjE1ZTE4ZTU5OTRjNWIzODgxNmFkNmYxNjA5MDk0OWY2NDE4NzQ1OGJjY2JiMjU4Nzk4YWQwYTkwNGM5NTMwMyJ9fX0=",
        4, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2Q3YjE2ZjVkMWVmNTE5ODg4ZDkzMmQ5MjlkYTllY2Y5ZjA5NTk5MGRiMjY2MDIyY2QzYTM3YWY3Y2YwMjVlYiJ9fX0=",
        5, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzRlZTdkOTU0ZWIxNGE1Y2NkMzQ2MjY2MjMxYmY5YTY3MTY1MjdiNTliYmNkNzk1NmNlZjA0YTlkOWIifX19",
        6, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGYxYmE1NTk5NzIyZGJhNjFkMTZmNzU5NjFmZTA5NGRlNDUxZDUxYjdkZGYwODdiZGNlNDFlZjkzMTk2ZDgxNiJ9fX0=",
        7, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTY5YTdmNTZlMWJmZWIwZDU5ZjU4OWUzNzc0MDZlYTViMWQxYzFkYzFjNDA4Nzk0ZDAzNWY3M2NmODhiNjdmNSJ9fX0=",
        8, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjU4NjlmNjJhYmM1ZGM3M2NlNzZiMGQ3OTAxMzZlNzQ3MTNhYTIxNWRmYWNmMWRmNzMxNzVjMTkxMDJlNzYzYiJ9fX0=",
        9, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGM5NjFkZDllZWI5NTk1MWZkYWQ2YzY2M2MyMmY4YzliNDE1ZGNlM2YxYjc0Y2M0Njk1MTkzNzYxMDEzIn19fQ=="
    );

    private final JavaPlugin plugin;
    private final PasswordManager passwordManager;
    private final Map<UUID, PinSession> sessions = new ConcurrentHashMap<>();

    public PinLoginMenu(JavaPlugin plugin, PasswordManager passwordManager) {
        this.plugin = plugin;
        this.passwordManager = passwordManager;
    }

    public boolean beginPinLogin(Player player, String[] args) {
        if (!passwordManager.prepareLoginAttempt(player, args)) {
            return false;
        }

        closeSilently(player);
        PinSession session = new PinSession(player);
        sessions.put(player.getUniqueId(), session);
        renderSession(session);
        player.openInventory(session.inventory);
        player.sendMessage(passwordManager.getPrefixedMessage("messages.pin-opened"));
        return true;
    }

    public void clearSession(Player player) {
        PinSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.closingSilently = true;
        }
    }

    public void shutdown() {
        for (PinSession session : sessions.values()) {
            session.closingSilently = true;
            HumanEntity viewer = session.player;
            if (viewer.getOpenInventory().getTopInventory().getHolder() == session) {
                viewer.closeInventory();
            }
        }
        sessions.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof PinSession session)) {
            return;
        }

        event.setCancelled(true);
        if (!player.getUniqueId().equals(session.player.getUniqueId())) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= session.inventory.getSize()) {
            return;
        }

        if (slot == CLEAR_SLOT) {
            session.enteredPin.setLength(0);
            renderSession(session);
            return;
        }
        if (slot == BACKSPACE_SLOT) {
            if (!session.enteredPin.isEmpty()) {
                session.enteredPin.deleteCharAt(session.enteredPin.length() - 1);
                renderSession(session);
            }
            return;
        }
        if (slot == CANCEL_SLOT) {
            session.closingSilently = true;
            sessions.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(passwordManager.getPrefixedMessage("messages.pin-closed"));
            return;
        }

        Integer digit = session.slotToDigit.get(slot);
        if (digit == null) {
            return;
        }

        session.enteredPin.append(digit);
        renderSession(session);
        if (session.enteredPin.length() >= passwordManager.getPinLength()) {
            completeEntry(session, player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof PinSession session)) {
            return;
        }

        sessions.remove(player.getUniqueId());
        if (!session.closingSilently) {
            player.sendMessage(passwordManager.getPrefixedMessage("messages.pin-closed"));
        }
    }

    private void completeEntry(PinSession session, Player player) {
        String enteredPin = session.enteredPin.toString();
        session.closingSilently = true;
        sessions.remove(player.getUniqueId());
        player.closeInventory();

        boolean success = passwordManager.handlePasswordInput(player, enteredPin);
        if (!success && player.isOnline() && passwordManager.isPinModeEnabled() && !passwordManager.isPlayerBlocked(player.getName())) {
            Bukkit.getScheduler().runTask(plugin, () -> beginPinLogin(player, new String[0]));
        }
    }

    private void closeSilently(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof PinSession session) {
            session.closingSilently = true;
            sessions.remove(player.getUniqueId());
            player.closeInventory();
        }
    }

    private void renderSession(PinSession session) {
        Inventory inventory = session.inventory;
        inventory.clear();

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, createFiller());
        }

        inventory.setItem(DISPLAY_SLOT, createDisplayItem(session.enteredPin.toString()));
        inventory.setItem(CLEAR_SLOT, createButton(Material.BARRIER, "&cClear", "&7Remove all entered digits"));
        inventory.setItem(BACKSPACE_SLOT, createButton(Material.ARROW, "&eBackspace", "&7Remove the last digit"));
        inventory.setItem(CANCEL_SLOT, createButton(Material.OAK_DOOR, "&cCancel", "&7Close the keypad"));

        List<Integer> digits = new ArrayList<>();
        for (int i = 0; i <= 9; i++) {
            digits.add(i);
        }
        Collections.shuffle(digits);
        session.slotToDigit.clear();

        for (int i = 0; i < DIGIT_SLOTS.length; i++) {
            int slot = DIGIT_SLOTS[i];
            int digit = digits.get(i);
            session.slotToDigit.put(slot, digit);
            inventory.setItem(slot, createDigitHead(digit));
        }
    }

    private ItemStack createDisplayItem(String enteredPin) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize("&bEntered PIN"));
        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7" + mask(enteredPin)));
        lore.add(colorize("&8Required length: " + passwordManager.getPinLength()));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String mask(String enteredPin) {
        if (enteredPin.isEmpty()) {
            return "_".repeat(Math.max(1, passwordManager.getPinLength()));
        }
        return "*".repeat(enteredPin.length()) + "_".repeat(Math.max(0, passwordManager.getPinLength() - enteredPin.length()));
    }

    private ItemStack createDigitHead(int digit) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setDisplayName(colorize("&6Digit " + digit));
        meta.setLore(List.of(
            colorize("&7Click to enter &f" + digit),
            colorize("&8" + CREDIT_LINE),
            colorize("&9" + CREDIT_URL)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        var profile = Bukkit.createProfile(UUID.randomUUID(), "oplogin-" + digit + "-" + UUID.randomUUID().toString().substring(0, 6).toLowerCase(Locale.ROOT));
        profile.setProperty(new ProfileProperty("textures", DIGIT_TEXTURES.get(digit)));
        meta.setPlayerProfile(profile);

        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createButton(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize(name));
        meta.setLore(List.of(colorize(loreLine)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private final class PinSession implements InventoryHolder {
        private final Player player;
        private final Inventory inventory;
        private final StringBuilder enteredPin = new StringBuilder();
        private final Map<Integer, Integer> slotToDigit = new ConcurrentHashMap<>();
        private boolean closingSilently;

        private PinSession(Player player) {
            this.player = player;
            this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE, passwordManager.getPinTitle());
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}

package ru.hubber.book;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.hubber.HubberPlugin;
import ru.hubber.config.ConfigManager;
import ru.hubber.utils.ColorUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BookManager {

    private final HubberPlugin plugin;
    private final ConfigManager config;
    private final NamespacedKey bookKey;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public BookManager(HubberPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.bookKey = new NamespacedKey(plugin, "hub_join_book");
    }

    public ItemStack createBookItem() {
        Material material = Material.matchMaterial(config.getBookMaterial());
        if (material == null) {
            material = Material.BOOK;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.toComponent(config.getBookName()));
            meta.lore(ColorUtils.toComponentList(config.getBookLore()));

            // Store identifier in PersistentDataContainer
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(bookKey, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isJoinBook(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(bookKey, PersistentDataType.BYTE)) {
            return true;
        }

        // Fallback check by display name component comparison
        if (meta.hasDisplayName()) {
            return meta.displayName() != null && meta.displayName().equals(ColorUtils.toComponent(config.getBookName()));
        }

        return false;
    }

    public void giveBook(Player player) {
        if (!config.isBookEnabled()) {
            return;
        }

        int slot = config.getBookSlot();
        ItemStack book = createBookItem();

        // Place in specific slot or first empty
        if (slot >= 0 && slot < 9) {
            player.getInventory().setItem(slot, book);
        } else {
            player.getInventory().addItem(book);
        }
    }

    public boolean checkCooldown(Player player) {
        long cooldownMs = config.getBookCooldownMs();
        if (cooldownMs <= 0) {
            return true;
        }

        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(player.getUniqueId());

        if (lastUse != null && (now - lastUse) < cooldownMs) {
            return false;
        }

        cooldowns.put(player.getUniqueId(), now);
        return true;
    }

    public void removeCooldown(UUID uuid) {
        cooldowns.remove(uuid);
    }

    public void executeBookAction(Player player) {
        if (!checkCooldown(player)) {
            return;
        }

        String command = config.getBookCommand();
        if (command == null || command.trim().isEmpty()) {
            return;
        }

        command = command.trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        // Execute command strictly from the player's perspective
        player.performCommand(command);
    }
}

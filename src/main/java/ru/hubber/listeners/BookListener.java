package ru.hubber.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import ru.hubber.book.BookManager;
import ru.hubber.config.ConfigManager;

public class BookListener implements Listener {

    private final ConfigManager config;
    private final BookManager bookManager;

    public BookListener(ConfigManager config, BookManager bookManager) {
        this.config = config;
        this.bookManager = bookManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!config.isBookEnabled()) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !bookManager.isJoinBook(item)) {
            return;
        }

        // Cancel the event so the vanilla book reading interface doesn't open
        event.setCancelled(true);

        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            bookManager.executeBookAction(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (!config.isBookEnabled() || !config.isBookPreventDrop()) {
            return;
        }

        ItemStack item = event.getItemDrop().getItemStack();
        if (bookManager.isJoinBook(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!config.isBookEnabled() || !config.isBookPreventMove()) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (bookManager.isJoinBook(current) || bookManager.isJoinBook(cursor)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (!config.isBookEnabled() || !config.isBookPreventMove()) {
            return;
        }

        if (bookManager.isJoinBook(event.getMainHandItem()) || bookManager.isJoinBook(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }
}

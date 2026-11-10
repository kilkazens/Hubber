package ru.hubber.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import ru.hubber.HubberPlugin;
import ru.hubber.book.BookManager;
import ru.hubber.collision.CollisionManager;
import ru.hubber.config.ConfigManager;
import ru.hubber.spawn.SpawnManager;

public class PlayerJoinListener implements Listener {

    private final HubberPlugin plugin;
    private final ConfigManager config;
    private final SpawnManager spawnManager;
    private final CollisionManager collisionManager;
    private final BookManager bookManager;

    public PlayerJoinListener(HubberPlugin plugin, ConfigManager config, SpawnManager spawnManager,
                              CollisionManager collisionManager, BookManager bookManager) {
        this.plugin = plugin;
        this.config = config;
        this.spawnManager = spawnManager;
        this.collisionManager = collisionManager;
        this.bookManager = bookManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. Force Survival Mode
        enforceSurvivalMode(player);

        // 2. Disable pushing / setup collision
        collisionManager.applyCollisionRule(player);

        // 3. Always spawn at /hubspawn regardless of where player logged off
        if (config.isAlwaysSpawnOnJoin() && spawnManager.isSpawnSet()) {
            Location spawn = spawnManager.getSpawnLocation();
            if (spawn != null) {
                player.teleport(spawn);
                // Also schedule 1 tick later to guarantee chunk sync
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.teleport(spawn);
                    }
                });
            }
        }

        // 4. Restore health and food
        try {
            org.bukkit.attribute.AttributeInstance healthAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (healthAttr != null) {
                player.setHealth(healthAttr.getValue());
            } else {
                player.setHealth(20.0);
            }
        } catch (Throwable ignored) {
            player.setHealth(20.0);
        }
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setFireTicks(0);

        // 5. Give join book
        if (config.isBookGiveOnJoin()) {
            bookManager.giveBook(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (config.isRespawnAtHubspawn() && spawnManager.isSpawnSet()) {
            Location spawn = spawnManager.getSpawnLocation();
            if (spawn != null) {
                event.setRespawnLocation(spawn);
            }
        }

        // Ensure collision rule and gamemode stay consistent after respawn
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                enforceSurvivalMode(player);
                collisionManager.applyCollisionRule(player);
                if (config.isBookGiveOnJoin()) {
                    bookManager.giveBook(player);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        boolean allowOp = config.isAllowOpsGamemodeBypass();
        boolean hasBypass = player.hasPermission(config.getGamemodeBypassPermission());

        if (config.isForceSurvival() && !(allowOp && player.isOp()) && !hasBypass) {
            if (event.getNewGameMode() != GameMode.SURVIVAL) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        collisionManager.removeCollisionRule(player);
        bookManager.removeCooldown(player.getUniqueId());
    }

    private void enforceSurvivalMode(Player player) {
        if (!config.isForceSurvival()) {
            return;
        }

        boolean allowOp = config.isAllowOpsGamemodeBypass();
        boolean hasBypass = player.hasPermission(config.getGamemodeBypassPermission());

        if ((allowOp && player.isOp()) || hasBypass) {
            return;
        }

        player.setGameMode(GameMode.SURVIVAL);
    }
}

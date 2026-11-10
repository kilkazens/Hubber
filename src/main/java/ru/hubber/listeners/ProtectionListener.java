package ru.hubber.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import ru.hubber.config.ConfigManager;
import ru.hubber.spawn.SpawnManager;
import ru.hubber.utils.ColorUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProtectionListener implements Listener {

    private final ConfigManager config;
    private final SpawnManager spawnManager;
    private final Map<UUID, Long> messageCooldowns = new ConcurrentHashMap<>();

    public ProtectionListener(ConfigManager config, SpawnManager spawnManager) {
        this.config = config;
        this.spawnManager = spawnManager;
    }

    // --- Block Breaking ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!config.isBlockBreakDenied()) {
            return;
        }

        Player player = event.getPlayer();
        boolean allowOp = config.isAllowOpsBreak();
        boolean hasBypass = player.hasPermission(config.getBlockBreakBypassPermission());

        if ((allowOp && player.isOp()) || hasBypass) {
            return;
        }

        event.setCancelled(true);
        if (config.isBlockBreakNotify()) {
            sendThrottledMessage(player, config.getBlockBreakMessage());
        }
    }

    // --- Block Placing ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!config.isBlockPlaceDenied()) {
            return;
        }

        Player player = event.getPlayer();
        boolean allowOp = config.isAllowOpsPlace();
        boolean hasBypass = player.hasPermission(config.getBlockPlaceBypassPermission());

        if ((allowOp && player.isOp()) || hasBypass) {
            return;
        }

        event.setCancelled(true);
        if (config.isBlockPlaceNotify()) {
            sendThrottledMessage(player, config.getBlockPlaceMessage());
        }
    }

    // --- Damage Prevention ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!config.isDisableDamage()) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            if (config.isVoidTeleportEnabled() && spawnManager.isSpawnSet()) {
                teleportSafelyToSpawn(player);
            }
            return;
        }

        event.setCancelled(true);
        player.setFireTicks(0);
    }

    // --- PvP Prevention ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!config.isDisablePvP()) {
            return;
        }

        // Damage target is a player
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
            return;
        }

        // Damager is a player
        if (event.getDamager() instanceof Player) {
            event.setCancelled(true);
            return;
        }

        // Damager is a projectile shot by a player
        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }

    // --- Hunger Prevention ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (config.isDisableHunger() && event.getEntity() instanceof Player player) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
        }
    }

    // --- Void Fall Teleport ---

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!config.isVoidTeleportEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getLocation().getY() <= config.getVoidTriggerY()) {
            if (spawnManager.isSpawnSet()) {
                teleportSafelyToSpawn(player);
            }
        }
    }

    // --- Item Drop Prevention ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (config.isPreventItemDrop()) {
            Player player = event.getPlayer();
            if (!player.isOp()) {
                event.setCancelled(true);
            }
        }
    }

    // --- Item Pickup Prevention ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (config.isPreventItemPickup() && event.getEntity() instanceof Player player) {
            if (!player.isOp()) {
                event.setCancelled(true);
            }
        }
    }

    // --- Farmland Trample & Physical Interact Prevention ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
            Player player = event.getPlayer();
            if (!player.isOp()) {
                event.setCancelled(true);
            }
        }
    }

    // --- Hanging Entities (Item Frames / Paintings) ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player) {
            if (!player.isOp()) {
                event.setCancelled(true);
            }
        }
    }

    private void teleportSafelyToSpawn(Player player) {
        player.setVelocity(new Vector(0, 0, 0));
        player.setFallDistance(0);
        spawnManager.teleportToSpawn(player, false);
    }

    private void sendThrottledMessage(Player player, String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastTime = messageCooldowns.get(player.getUniqueId());

        if (lastTime != null && (now - lastTime) < 1500) {
            return;
        }

        messageCooldowns.put(player.getUniqueId(), now);
        player.sendMessage(ColorUtils.colorize(message));
    }
}

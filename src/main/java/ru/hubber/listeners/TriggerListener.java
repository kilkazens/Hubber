package ru.hubber.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import ru.hubber.config.ConfigManager;
import ru.hubber.spawn.SpawnManager;
import ru.hubber.trigger.Trigger;
import ru.hubber.trigger.TriggerManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TriggerListener implements Listener {

    private final ConfigManager config;
    private final SpawnManager spawnManager;
    private final TriggerManager triggerManager;
    private final Map<UUID, Long> triggerCooldowns = new ConcurrentHashMap<>();

    public TriggerListener(ConfigManager config, SpawnManager spawnManager, TriggerManager triggerManager) {
        this.config = config;
        this.spawnManager = spawnManager;
        this.triggerManager = triggerManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        // Optimization: only process when player moves between blocks
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        if (!spawnManager.isSpawnSet()) {
            return;
        }

        Player player = event.getPlayer();

        // Prevent infinite loops if player is right at spawn
        Location spawn = spawnManager.getSpawnLocation();
        if (spawn != null && to.getWorld() != null && to.getWorld().equals(spawn.getWorld())) {
            if (to.distanceSquared(spawn) < 9.0) {
                return;
            }
        }

        // Check trigger cooldown (1.5 seconds)
        long now = System.currentTimeMillis();
        Long lastTrigger = triggerCooldowns.get(player.getUniqueId());
        if (lastTrigger != null && (now - lastTrigger) < 1500) {
            return;
        }

        Trigger hit = triggerManager.checkTrigger(to);
        if (hit != null) {
            triggerCooldowns.put(player.getUniqueId(), now);

            player.setVelocity(new Vector(0, 0, 0));
            player.setFallDistance(0);

            // Teleport player to hubspawn
            spawnManager.teleportToSpawn(player, false);

            // Send notification message if configured
            String msg = config.getTriggerTeleportMessage();
            if (msg != null && !msg.trim().isEmpty()) {
                player.sendMessage(msg);
            }

            // Play sound if configured
            if (config.isTriggerPlaySound()) {
                try {
                    String soundKey = config.getTriggerSound().toLowerCase().replace('_', '.');
                    if (!soundKey.contains(":")) {
                        soundKey = "minecraft:" + soundKey;
                    }
                    net.kyori.adventure.key.Key key = net.kyori.adventure.key.Key.key(soundKey);
                    net.kyori.adventure.sound.Sound sound = net.kyori.adventure.sound.Sound.sound(
                            key,
                            net.kyori.adventure.sound.Sound.Source.PLAYER,
                            1.0f,
                            1.0f
                    );
                    player.playSound(sound);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        triggerCooldowns.remove(event.getPlayer().getUniqueId());
    }
}

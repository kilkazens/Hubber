package ru.hubber.spawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.hubber.HubberPlugin;
import ru.hubber.config.ConfigManager;

public class SpawnManager {

    private final HubberPlugin plugin;
    private final ConfigManager config;
    private Location cachedSpawn;

    public SpawnManager(HubberPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        loadSpawn();
    }

    public void loadSpawn() {
        if (!config.isSpawnSet()) {
            this.cachedSpawn = null;
            return;
        }

        World world = Bukkit.getWorld(config.getSpawnWorld());
        if (world == null) {
            plugin.getLogger().warning("Мир точки спавна '" + config.getSpawnWorld() + "' не найден!");
            this.cachedSpawn = null;
            return;
        }

        this.cachedSpawn = new Location(
                world,
                config.getSpawnX(),
                config.getSpawnY(),
                config.getSpawnZ(),
                config.getSpawnYaw(),
                config.getSpawnPitch()
        );
    }

    public boolean isSpawnSet() {
        return cachedSpawn != null;
    }

    public Location getSpawnLocation() {
        return cachedSpawn;
    }

    public void setSpawn(Location location) {
        this.cachedSpawn = location.clone();
        config.setSpawn(location);
    }

    public boolean teleportToSpawn(Player player) {
        return teleportToSpawn(player, true);
    }

    public boolean teleportToSpawn(Player player, boolean notify) {
        if (!isSpawnSet()) {
            if (notify) {
                player.sendMessage(config.getFormattedMessage("spawn_not_set"));
            }
            return false;
        }

        player.teleport(cachedSpawn);
        if (notify) {
            String msg = config.getFormattedMessage("teleported_to_spawn");
            if (!msg.trim().isEmpty()) {
                player.sendMessage(msg);
            }
        }
        return true;
    }
}

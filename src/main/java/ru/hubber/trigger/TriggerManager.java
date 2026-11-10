package ru.hubber.trigger;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.hubber.HubberPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TriggerManager {

    private final HubberPlugin plugin;
    private final File triggersFile;
    private final Map<String, Trigger> triggers = new ConcurrentHashMap<>();

    // Selections for cuboid trigger creation
    private final Map<UUID, Location> pos1Selections = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pos2Selections = new ConcurrentHashMap<>();

    public TriggerManager(HubberPlugin plugin) {
        this.plugin = plugin;
        this.triggersFile = new File(plugin.getDataFolder(), "triggers.yml");
        loadTriggers();
    }

    public void loadTriggers() {
        triggers.clear();
        if (!triggersFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(triggersFile);
        ConfigurationSection root = config.getConfigurationSection("triggers");
        if (root == null) {
            return;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec != null) {
                try {
                    Trigger trigger = Trigger.loadFromSection(key, sec);
                    triggers.put(key.toLowerCase(), trigger);
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка при загрузке триггера '" + key + "': " + e.getMessage());
                }
            }
        }
    }

    public void saveTriggers() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("triggers");

        for (Trigger trigger : triggers.values()) {
            ConfigurationSection sec = root.createSection(trigger.getId());
            trigger.saveToSection(sec);
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(triggersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить triggers.yml: " + e.getMessage());
        }
    }

    public void addSphereTrigger(String name, Location center, double radius) {
        String id = name.trim().toLowerCase();
        Trigger trigger = new Trigger(id, center.getWorld().getName(),
                center.getX(), center.getY(), center.getZ(), radius);
        triggers.put(id, trigger);
        saveTriggers();
    }

    public void addCuboidTrigger(String name, Location p1, Location p2) {
        String id = name.trim().toLowerCase();
        Trigger trigger = new Trigger(id, p1.getWorld().getName(),
                p1.getX(), p1.getY(), p1.getZ(),
                p2.getX(), p2.getY(), p2.getZ());
        triggers.put(id, trigger);
        saveTriggers();
    }

    public boolean removeTrigger(String name) {
        String id = name.trim().toLowerCase();
        Trigger removed = triggers.remove(id);
        if (removed != null) {
            saveTriggers();
            return true;
        }
        return false;
    }

    public Trigger checkTrigger(Location loc) {
        for (Trigger trigger : triggers.values()) {
            if (trigger.contains(loc)) {
                return trigger;
            }
        }
        return null;
    }

    public Collection<Trigger> getTriggers() {
        return triggers.values();
    }

    public Trigger getTrigger(String name) {
        return triggers.get(name.trim().toLowerCase());
    }

    public void setPos1(UUID playerId, Location loc) {
        pos1Selections.put(playerId, loc.clone());
    }

    public void setPos2(UUID playerId, Location loc) {
        pos2Selections.put(playerId, loc.clone());
    }

    public Location getPos1(UUID playerId) {
        return pos1Selections.get(playerId);
    }

    public Location getPos2(UUID playerId) {
        return pos2Selections.get(playerId);
    }

    public void clearSelection(UUID playerId) {
        pos1Selections.remove(playerId);
        pos2Selections.remove(playerId);
    }
}

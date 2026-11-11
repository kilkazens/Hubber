package ru.hubber.npc;

import com.destroystokyo.paper.SkinParts;
import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerTextures;
import ru.hubber.HubberPlugin;
import ru.hubber.utils.ColorUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NpcManager {

    private final HubberPlugin plugin;
    private final NamespacedKey keyNpcCmd;
    private final NamespacedKey keyNpcId;
    private final File npcsFile;
    private final Map<String, NpcData> npcs = new ConcurrentHashMap<>();

    public NpcManager(HubberPlugin plugin) {
        this.plugin = plugin;
        this.keyNpcCmd = new NamespacedKey(plugin, "npc_cmd");
        this.keyNpcId = new NamespacedKey(plugin, "npc_id");
        this.npcsFile = new File(plugin.getDataFolder(), "npcs.yml");

        loadNpcs();
    }

    public NamespacedKey getKeyNpcCmd() {
        return keyNpcCmd;
    }

    public NamespacedKey getKeyNpcId() {
        return keyNpcId;
    }

    public void loadNpcs() {
        npcs.clear();
        if (!npcsFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(npcsFile);
        ConfigurationSection root = config.getConfigurationSection("npcs");
        if (root == null) {
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec != null) {
                try {
                    String worldName = sec.getString("world", "world");
                    double x = sec.getDouble("x");
                    double y = sec.getDouble("y");
                    double z = sec.getDouble("z");
                    float yaw = (float) sec.getDouble("yaw", 0.0);
                    float pitch = (float) sec.getDouble("pitch", 0.0);
                    String command = sec.getString("command", "joinserver");
                    String skin = sec.getString("skin", "default");
                    String name = sec.getString("name", "NPC");
                    String color = sec.getString("color", "white");
                    boolean bold = sec.getBoolean("bold", false);
                    boolean italic = sec.getBoolean("italic", false);

                    String uuidStr = sec.getString("entity_uuid");
                    UUID entityUuid = uuidStr != null ? UUID.fromString(uuidStr) : UUID.randomUUID();

                    NpcData data = new NpcData(id, entityUuid, worldName, x, y, z, yaw, pitch,
                            command, skin, name, color, bold, italic);
                    npcs.put(id.toLowerCase(), data);

                    // Check if entity is already in world, or schedule spawn
                    respawnIfMissing(data);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load NPC '" + id + "': " + e.getMessage());
                }
            }
        }
    }

    public void saveNpcs() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("npcs");

        for (NpcData data : npcs.values()) {
            ConfigurationSection sec = root.createSection(data.getId());
            sec.set("world", data.getWorldName());
            sec.set("x", data.getX());
            sec.set("y", data.getY());
            sec.set("z", data.getZ());
            sec.set("yaw", data.getYaw());
            sec.set("pitch", data.getPitch());
            sec.set("command", data.getCommand());
            sec.set("skin", data.getSkinInput());
            sec.set("name", data.getDisplayName());
            sec.set("color", data.getColor());
            sec.set("bold", data.isBold());
            sec.set("italic", data.isItalic());
            sec.set("entity_uuid", data.getEntityUuid().toString());
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(npcsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save npcs.yml: " + e.getMessage());
        }
    }

    public NpcData createNpc(Player player, String command, String skin, String name,
                            String color, boolean bold, boolean italic) {
        String id = generateAutoId();
        Location loc = player.getLocation();
        World world = loc.getWorld();

        if (world == null) {
            return null;
        }

        // 1. Spawn native Mannequin entity
        Mannequin mannequin = (Mannequin) world.spawnEntity(loc, EntityType.MANNEQUIN);
        setupMannequinProperties(mannequin, id, command, skin, name, color, bold, italic);

        NpcData data = new NpcData(id, mannequin.getUniqueId(), world.getName(),
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(),
                command, skin, name, color, bold, italic);

        npcs.put(id.toLowerCase(), data);
        saveNpcs();
        return data;
    }

    private void setupMannequinProperties(Mannequin mannequin, String id, String command,
                                         String skin, String name, String color,
                                         boolean bold, boolean italic) {
        // Tag with PDC
        PersistentDataContainer pdc = mannequin.getPersistentDataContainer();
        pdc.set(keyNpcId, PersistentDataType.STRING, id);
        pdc.set(keyNpcCmd, PersistentDataType.STRING, command);

        // Formatting
        TextColor textColor = ColorUtils.parseTextColor(color);
        Component nameComponent = Component.text(name.replace('_', ' '))
                .color(textColor)
                .decoration(TextDecoration.BOLD, bold)
                .decoration(TextDecoration.ITALIC, italic);

        mannequin.customName(nameComponent);
        mannequin.setCustomNameVisible(true);

        // Entity configuration
        mannequin.setSkinParts(SkinParts.allParts());
        mannequin.setImmovable(true);
        mannequin.setInvulnerable(true);
        mannequin.setGravity(false);
        mannequin.setPersistent(true);
        mannequin.setRemoveWhenFarAway(false);
        mannequin.setSilent(true);

        // Apply skin
        applySkin(mannequin, skin, name);
    }

    private void applySkin(Mannequin mannequin, String skinInput, String defaultName) {
        if (skinInput == null || skinInput.equalsIgnoreCase("none") || skinInput.equalsIgnoreCase("default")) {
            return;
        }

        String target = skinInput.trim();

        // 1. If it's a direct URL
        if (target.startsWith("http://") || target.startsWith("https://")) {
            applySkinUrl(mannequin, target);
            return;
        }

        // 2. If it's a 64-char texture hash (Mojang textures server)
        if (target.matches("^[a-fA-F0-9]{64}$")) {
            applySkinUrl(mannequin, "http://textures.minecraft.net/texture/" + target);
            return;
        }

        // 3. Otherwise treat as a player username and resolve profile asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerProfile profile = Bukkit.createProfile(target);
                if (profile.complete()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (mannequin.isValid()) {
                            mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
                        }
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not complete profile for player skin '" + target + "': " + e.getMessage());
            }
        });
    }

    private void applySkinUrl(Mannequin mannequin, String urlStr) {
        try {
            URL url = URI.create(urlStr).toURL();
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "NPC");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(url);
            profile.setTextures(textures);
            mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid skin URL '" + urlStr + "': " + e.getMessage());
        }
    }

    private void respawnIfMissing(NpcData data) {
        World world = Bukkit.getWorld(data.getWorldName());
        if (world == null) {
            return;
        }

        Entity existing = Bukkit.getEntity(data.getEntityUuid());
        if (existing instanceof Mannequin m && m.isValid()) {
            return;
        }

        // Spawn missing mannequin
        Location loc = new Location(world, data.getX(), data.getY(), data.getZ(), data.getYaw(), data.getPitch());
        Mannequin mannequin = (Mannequin) world.spawnEntity(loc, EntityType.MANNEQUIN);
        setupMannequinProperties(mannequin, data.getId(), data.getCommand(),
                data.getSkinInput(), data.getDisplayName(), data.getColor(),
                data.isBold(), data.isItalic());
    }

    public boolean removeNpc(String id) {
        String key = id.trim().toLowerCase();
        NpcData data = npcs.remove(key);
        if (data == null) {
            return false;
        }

        // Remove entity from world
        World world = Bukkit.getWorld(data.getWorldName());
        if (world != null) {
            Entity entity = Bukkit.getEntity(data.getEntityUuid());
            if (entity != null) {
                entity.remove();
            } else {
                for (Entity e : world.getEntitiesByClass(Mannequin.class)) {
                    PersistentDataContainer pdc = e.getPersistentDataContainer();
                    String storedId = pdc.get(keyNpcId, PersistentDataType.STRING);
                    if (key.equalsIgnoreCase(storedId)) {
                        e.remove();
                    }
                }
            }
        }

        saveNpcs();
        return true;
    }

    public NpcData removeNearestNpc(Player player, double maxDist) {
        Location loc = player.getLocation();
        double maxDistSq = maxDist * maxDist;
        NpcData nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (NpcData data : npcs.values()) {
            if (data.getWorldName().equals(loc.getWorld().getName())) {
                double dx = data.getX() - loc.getX();
                double dy = data.getY() - loc.getY();
                double dz = data.getZ() - loc.getZ();
                double dSq = dx * dx + dy * dy + dz * dz;
                if (dSq <= maxDistSq && dSq < nearestDistSq) {
                    nearestDistSq = dSq;
                    nearest = data;
                }
            }
        }

        if (nearest != null) {
            removeNpc(nearest.getId());
            return nearest;
        }

        return null;
    }

    public Collection<NpcData> getNpcs() {
        return npcs.values();
    }

    public NpcData getNpc(String id) {
        return npcs.get(id.trim().toLowerCase());
    }

    private String generateAutoId() {
        int index = 1;
        while (npcs.containsKey("npc_" + index)) {
            index++;
        }
        return "npc_" + index;
    }
}

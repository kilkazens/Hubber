package ru.hubber.config;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.hubber.HubberPlugin;
import ru.hubber.utils.ColorUtils;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class ConfigManager {

    private final HubberPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration activeLangConfig;
    private FileConfiguration defaultLangConfig;

    public ConfigManager(HubberPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        loadLanguages();
    }

    private void loadLanguages() {
        // Save default language files if not present
        saveResourceIfNotExists("messages_en.yml");
        saveResourceIfNotExists("messages_ru.yml");

        // Load fallback language (English)
        File enFile = new File(plugin.getDataFolder(), "messages_en.yml");
        if (enFile.exists()) {
            this.defaultLangConfig = YamlConfiguration.loadConfiguration(enFile);
        } else {
            var resource = plugin.getResource("messages_en.yml");
            if (resource != null) {
                this.defaultLangConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
            }
        }

        // Determine active language
        String langCode = config.getString("language", "en").trim().toLowerCase();
        if (!langCode.equals("en") && !langCode.equals("ru")) {
            plugin.getLogger().warning("Unknown language '" + langCode + "'. Falling back to 'en'.");
            langCode = "en";
        }

        File activeFile = new File(plugin.getDataFolder(), "messages_" + langCode + ".yml");
        if (activeFile.exists()) {
            this.activeLangConfig = YamlConfiguration.loadConfiguration(activeFile);
        } else {
            this.activeLangConfig = this.defaultLangConfig;
        }
    }

    private void saveResourceIfNotExists(String resourceName) {
        File file = new File(plugin.getDataFolder(), resourceName);
        if (!file.exists()) {
            try {
                plugin.saveResource(resourceName, false);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save resource " + resourceName + ": " + e.getMessage());
            }
        }
    }

    public String getRawMessage(String path) {
        if (activeLangConfig != null && activeLangConfig.contains(path)) {
            return activeLangConfig.getString(path);
        }
        if (defaultLangConfig != null && defaultLangConfig.contains(path)) {
            return defaultLangConfig.getString(path);
        }
        return "";
    }

    public List<String> getRawMessageList(String path) {
        if (activeLangConfig != null && activeLangConfig.contains(path)) {
            return activeLangConfig.getStringList(path);
        }
        if (defaultLangConfig != null && defaultLangConfig.contains(path)) {
            return defaultLangConfig.getStringList(path);
        }
        return Collections.emptyList();
    }

    public String getPrefix() {
        return ColorUtils.colorize(getRawMessage("prefix"));
    }

    public String getMessage(String key) {
        String msg = getRawMessage(key);
        if (msg == null || msg.isEmpty()) {
            return "";
        }
        return ColorUtils.colorize(msg);
    }

    public String getFormattedMessage(String key) {
        return getPrefix() + getMessage(key);
    }

    // --- Proxy / JoinServer Settings ---

    public String getTargetServer() {
        return config.getString("joinserver.target_server", "main");
    }

    // --- Spawn Settings ---

    public boolean isSpawnSet() {
        return config.getBoolean("spawn.set", false);
    }

    public String getSpawnWorld() {
        return config.getString("spawn.world", "world");
    }

    public double getSpawnX() {
        return config.getDouble("spawn.x", 0.5);
    }

    public double getSpawnY() {
        return config.getDouble("spawn.y", 100.0);
    }

    public double getSpawnZ() {
        return config.getDouble("spawn.z", 0.5);
    }

    public float getSpawnYaw() {
        return (float) config.getDouble("spawn.yaw", 0.0);
    }

    public float getSpawnPitch() {
        return (float) config.getDouble("spawn.pitch", 0.0);
    }

    public boolean isAlwaysSpawnOnJoin() {
        return config.getBoolean("spawn.always_spawn_on_join", true);
    }

    public boolean isRespawnAtHubspawn() {
        return config.getBoolean("spawn.respawn_at_hubspawn", true);
    }

    public void setSpawn(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        config.set("spawn.set", true);
        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        config.set("spawn.yaw", location.getYaw());
        config.set("spawn.pitch", location.getPitch());
        plugin.saveConfig();
    }

    // --- Gamemode Settings ---

    public boolean isForceSurvival() {
        return config.getBoolean("gamemode.force_survival", true);
    }

    public boolean isAllowOpsGamemodeBypass() {
        return config.getBoolean("gamemode.allow_ops_bypass", true);
    }

    public String getGamemodeBypassPermission() {
        return config.getString("gamemode.bypass_permission", "hubber.bypass.gamemode");
    }

    // --- Book Settings ---

    public boolean isBookEnabled() {
        return config.getBoolean("book.enabled", true);
    }

    public int getBookSlot() {
        return config.getInt("book.slot", 0);
    }

    public boolean isBookGiveOnJoin() {
        return config.getBoolean("book.give_on_join", true);
    }

    public boolean isBookPreventDrop() {
        return config.getBoolean("book.prevent_drop", true);
    }

    public boolean isBookPreventMove() {
        return config.getBoolean("book.prevent_move", false);
    }

    public String getBookCommand() {
        return config.getString("book.command", "joinserver");
    }

    public long getBookCooldownMs() {
        return config.getLong("book.cooldown_ms", 1000L);
    }

    public String getBookMaterial() {
        return config.getString("book.material", "BOOK");
    }

    public String getBookName() {
        return getRawMessage("book.name");
    }

    public List<String> getBookLore() {
        return getRawMessageList("book.lore");
    }

    // --- Protection Settings ---

    public boolean isBlockBreakDenied() {
        return config.getBoolean("protection.block_break.deny", true);
    }

    public boolean isAllowOpsBreak() {
        return config.getBoolean("protection.block_break.allow_ops", true);
    }

    public String getBlockBreakBypassPermission() {
        return config.getString("protection.block_break.bypass_permission", "hubber.bypass.break");
    }

    public boolean isBlockBreakNotify() {
        return config.getBoolean("protection.block_break.notify", true);
    }

    public String getBlockBreakMessage() {
        return getMessage("protection.block_break_denied");
    }

    public boolean isBlockPlaceDenied() {
        return config.getBoolean("protection.block_place.deny", true);
    }

    public boolean isAllowOpsPlace() {
        return config.getBoolean("protection.block_place.allow_ops", true);
    }

    public String getBlockPlaceBypassPermission() {
        return config.getString("protection.block_place.bypass_permission", "hubber.bypass.place");
    }

    public boolean isBlockPlaceNotify() {
        return config.getBoolean("protection.block_place.notify", true);
    }

    public String getBlockPlaceMessage() {
        return getMessage("protection.block_place_denied");
    }

    public boolean isDisableDamage() {
        return config.getBoolean("protection.disable_damage", true);
    }

    public boolean isDisablePvP() {
        return config.getBoolean("protection.disable_pvp", true);
    }

    public boolean isDisableCollisions() {
        return config.getBoolean("protection.disable_collisions", true);
    }

    public boolean isDisableHunger() {
        return config.getBoolean("protection.disable_hunger", true);
    }

    public boolean isPreventItemDrop() {
        return config.getBoolean("protection.prevent_item_drop", true);
    }

    public boolean isPreventItemPickup() {
        return config.getBoolean("protection.prevent_item_pickup", true);
    }

    public boolean isVoidTeleportEnabled() {
        return config.getBoolean("protection.void_teleport.enabled", true);
    }

    public double getVoidTriggerY() {
        return config.getDouble("protection.void_teleport.trigger_y", -64.0);
    }

    // --- Verification Settings ---

    public String getVerificationLink() {
        return config.getString("verification.link", "https://discord.gg/yourserver");
    }

    public String getVerificationHoverText() {
        return getRawMessage("verification.hover_text");
    }

    public List<String> getVerificationMessage() {
        return getRawMessageList("verification.message");
    }

    // --- Trigger Settings ---

    public boolean isTriggerPlaySound() {
        return config.getBoolean("triggers.play_sound", true);
    }

    public String getTriggerSound() {
        return config.getString("triggers.sound", "entity.enderman.teleport");
    }

    public double getTriggerDefaultRadius() {
        return config.getDouble("triggers.default_radius", 2.0);
    }

    public String getTriggerTeleportMessage() {
        return getMessage("triggers.teleported");
    }

    // --- Chat Settings ---

    public boolean isDisableJoinQuitMessages() {
        return config.getBoolean("chat.disable_join_quit_messages", true);
    }

    // --- Server Monitor Settings ---

    public boolean isServerMonitorEnabled() {
        return config.getBoolean("server_monitor.enabled", true);
    }

    public String getServerMonitorHost() {
        return config.getString("server_monitor.host", "127.0.0.1");
    }

    public int getServerMonitorPort() {
        return config.getInt("server_monitor.port", 25566);
    }

    public int getServerMonitorCheckInterval() {
        return config.getInt("server_monitor.check_interval_seconds", 3);
    }

    public int getServerMonitorTimeoutMs() {
        return config.getInt("server_monitor.timeout_ms", 1500);
    }

    public boolean isServerMonitorShowBossBar() {
        return config.getBoolean("server_monitor.show_bossbar", true);
    }

    public boolean isServerMonitorSendTitle() {
        return config.getBoolean("server_monitor.send_title_on_online", true);
    }

    public boolean isServerMonitorBroadcastChat() {
        return config.getBoolean("server_monitor.broadcast_chat", true);
    }

    public boolean isServerMonitorPlaySound() {
        return config.getBoolean("server_monitor.play_sound", true);
    }

    public String getServerMonitorSound() {
        return config.getString("server_monitor.sound", "entity.player.levelup");
    }

    public boolean isServerMonitorBlockJoinWhileOffline() {
        return config.getBoolean("server_monitor.block_join_while_offline", true);
    }
}

package ru.hubber.broadcast;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import ru.hubber.HubberPlugin;
import ru.hubber.utils.ColorUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BroadcastManager {

    private static final Pattern BRACKET_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");

    private final HubberPlugin plugin;
    private final File broadcastsFile;

    private boolean enabled = false;
    private int intervalSeconds = 120;
    private boolean random = false;
    private boolean playSound = true;
    private String soundKey = "entity.experience_orb.pickup";

    private final List<BroadcastItem> broadcastList = new ArrayList<>();
    private int currentIndex = 0;
    private BukkitTask task;

    public BroadcastManager(HubberPlugin plugin) {
        this.plugin = plugin;
        this.broadcastsFile = new File(plugin.getDataFolder(), "broadcasts.yml");
        loadBroadcasts();
    }

    public void loadBroadcasts() {
        stop();
        broadcastList.clear();

        // Save default broadcasts.yml if missing
        if (!broadcastsFile.exists()) {
            try {
                plugin.saveResource("broadcasts.yml", false);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save default broadcasts.yml: " + e.getMessage());
            }
        }

        if (!broadcastsFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(broadcastsFile);
        this.enabled = config.getBoolean("enabled", false);
        this.intervalSeconds = config.getInt("interval_seconds", 120);
        this.random = config.getBoolean("random", false);
        this.playSound = config.getBoolean("play_sound", true);
        this.soundKey = config.getString("sound", "entity.experience_orb.pickup");

        ConfigurationSection section = config.getConfigurationSection("broadcasts");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection itemSec = section.getConfigurationSection(id);
                if (itemSec != null) {
                    List<String> messages = itemSec.getStringList("messages");
                    if (messages.isEmpty() && itemSec.isString("messages")) {
                        messages = List.of(itemSec.getString("messages"));
                    }
                    String clickAction = itemSec.getString("click_action", "");
                    String clickValue = itemSec.getString("click_value", "");
                    String hoverText = itemSec.getString("hover_text", "");

                    broadcastList.add(new BroadcastItem(id, messages, clickAction, clickValue, hoverText));
                }
            }
        }

        start();
    }

    public void start() {
        stop();

        if (!enabled || broadcastList.isEmpty() || intervalSeconds <= 0) {
            return;
        }

        long intervalTicks = Math.max(20L, intervalSeconds * 20L);
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::broadcastNext, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void reload() {
        loadBroadcasts();
    }

    public void broadcastNext() {
        if (broadcastList.isEmpty()) {
            return;
        }

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        BroadcastItem item;
        if (random) {
            int index = ThreadLocalRandom.current().nextInt(broadcastList.size());
            item = broadcastList.get(index);
        } else {
            if (currentIndex >= broadcastList.size()) {
                currentIndex = 0;
            }
            item = broadcastList.get(currentIndex);
            currentIndex = (currentIndex + 1) % broadcastList.size();
        }

        sendBroadcast(item);
    }

    public boolean broadcastById(String id) {
        for (BroadcastItem item : broadcastList) {
            if (item.id().equalsIgnoreCase(id.trim())) {
                sendBroadcast(item);
                return true;
            }
        }
        return false;
    }

    public void sendBroadcast(BroadcastItem item) {
        if (item == null || item.messages().isEmpty()) {
            return;
        }

        // Build ClickEvent & HoverEvent if specified
        ClickEvent clickEvent = createClickEvent(item.clickAction(), item.clickValue());
        HoverEvent<?> hoverEvent = (item.hoverText() != null && !item.hoverText().isEmpty())
                ? HoverEvent.showText(ColorUtils.toComponent(item.hoverText()))
                : null;

        // Broadcast lines to all online players
        for (String rawLine : item.messages()) {
            Component lineComponent = formatLine(rawLine, item.clickValue(), clickEvent, hoverEvent);
            Bukkit.broadcast(lineComponent);
        }

        // Play chime sound if enabled
        if (playSound && soundKey != null && !soundKey.isEmpty()) {
            try {
                Sound sound = Sound.sound(Key.key(soundKey), Sound.Source.MASTER, 1.0f, 1.0f);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(sound);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private Component formatLine(String line, String clickValue, ClickEvent clickEvent, HoverEvent<?> hoverEvent) {
        if (clickEvent == null) {
            return ColorUtils.toComponent(line);
        }

        // 1. Bracket syntax e.g. "[Click here]" -> interactive button
        Matcher matcher = BRACKET_PATTERN.matcher(line);
        if (matcher.find()) {
            TextComponent.Builder lineBuilder = Component.text();
            int lastEnd = 0;
            matcher.reset();

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();

                if (start > lastEnd) {
                    lineBuilder.append(ColorUtils.toComponent(line.substring(lastEnd, start)));
                }

                String bracketContent = matcher.group(1);
                Component button = ColorUtils.toComponent("[" + bracketContent + "]")
                        .clickEvent(clickEvent);
                if (hoverEvent != null) {
                    button = button.hoverEvent(hoverEvent);
                }
                lineBuilder.append(button);
                lastEnd = end;
            }

            if (lastEnd < line.length()) {
                lineBuilder.append(ColorUtils.toComponent(line.substring(lastEnd)));
            }
            return lineBuilder.build();
        }

        // 2. {link} or {url} placeholder syntax
        if (line.contains("{link}") || line.contains("{url}")) {
            String token = line.contains("{link}") ? "{link}" : "{url}";
            String[] parts = line.split(Pattern.quote(token), -1);
            TextComponent.Builder lineBuilder = Component.text();

            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    lineBuilder.append(ColorUtils.toComponent(parts[i]));
                }
                if (i < parts.length - 1) {
                    Component linkComp = ColorUtils.toComponent(clickValue).clickEvent(clickEvent);
                    if (hoverEvent != null) {
                        linkComp = linkComp.hoverEvent(hoverEvent);
                    }
                    lineBuilder.append(linkComp);
                }
            }
            return lineBuilder.build();
        }

        return ColorUtils.toComponent(line);
    }

    private ClickEvent createClickEvent(String action, String value) {
        if (action == null || action.trim().isEmpty() || value == null || value.trim().isEmpty()) {
            return null;
        }

        String act = action.trim().toUpperCase();
        String val = value.trim();

        return switch (act) {
            case "OPEN_URL", "URL", "LINK" -> ClickEvent.openUrl(val);
            case "RUN_COMMAND", "COMMAND", "CMD" -> ClickEvent.runCommand(val.startsWith("/") ? val : "/" + val);
            case "SUGGEST_COMMAND", "SUGGEST" -> ClickEvent.suggestCommand(val.startsWith("/") ? val : "/" + val);
            case "COPY_TO_CLIPBOARD", "COPY" -> ClickEvent.copyToClipboard(val);
            default -> null;
        };
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<BroadcastItem> getBroadcastList() {
        return broadcastList;
    }
}

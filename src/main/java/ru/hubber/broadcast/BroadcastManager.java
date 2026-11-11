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

    // Periodic auto-broadcast settings
    private boolean enabled = false;
    private int intervalSeconds = 120;
    private boolean random = false;
    private boolean playSound = true;
    private String soundKey = "entity.experience_orb.pickup";
    private final List<BroadcastItem> broadcastList = new ArrayList<>();
    private int currentIndex = 0;
    private BukkitTask task;

    // Join broadcast settings
    private boolean joinEnabled = false;
    private String joinSendTo = "PLAYER";
    private int joinDelayTicks = 15;
    private boolean joinPlaySound = true;
    private String joinSoundKey = "entity.player.levelup";
    private BroadcastItem joinBroadcastItem;

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

        // 1. Periodic broadcasts settings
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

        // 2. Join broadcast settings
        ConfigurationSection joinSec = config.getConfigurationSection("join_broadcast");
        if (joinSec != null) {
            this.joinEnabled = joinSec.getBoolean("enabled", false);
            this.joinSendTo = joinSec.getString("send_to", "PLAYER");
            this.joinDelayTicks = joinSec.getInt("delay_ticks", 15);
            this.joinPlaySound = joinSec.getBoolean("play_sound", true);
            this.joinSoundKey = joinSec.getString("sound", "entity.player.levelup");

            List<String> messages = joinSec.getStringList("messages");
            if (messages.isEmpty() && joinSec.isString("messages")) {
                messages = List.of(joinSec.getString("messages"));
            }
            String clickAction = joinSec.getString("click_action", "");
            String clickValue = joinSec.getString("click_value", "");
            String hoverText = joinSec.getString("hover_text", "");

            this.joinBroadcastItem = new BroadcastItem("join", messages, clickAction, clickValue, hoverText);
        } else {
            this.joinEnabled = false;
            this.joinBroadcastItem = null;
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

    public void handlePlayerJoin(Player player) {
        if (!joinEnabled || joinBroadcastItem == null || joinBroadcastItem.messages().isEmpty()) {
            return;
        }

        long delay = Math.max(1L, joinDelayTicks);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            sendJoinBroadcast(player);
        }, delay);
    }

    public void sendJoinBroadcast(Player player) {
        if (joinBroadcastItem == null || joinBroadcastItem.messages().isEmpty()) {
            return;
        }

        boolean sendToAll = "ALL".equalsIgnoreCase(joinSendTo);
        ClickEvent clickEvent = createClickEvent(joinBroadcastItem.clickAction(), joinBroadcastItem.clickValue());
        HoverEvent<?> hoverEvent = (joinBroadcastItem.hoverText() != null && !joinBroadcastItem.hoverText().isEmpty())
                ? HoverEvent.showText(ColorUtils.toComponent(joinBroadcastItem.hoverText()))
                : null;

        if (sendToAll) {
            // Broadcast to all online players
            for (String rawLine : joinBroadcastItem.messages()) {
                Component lineComp = formatLine(rawLine, joinBroadcastItem.clickValue(), clickEvent, hoverEvent, player);
                Bukkit.broadcast(lineComp);
            }
            if (joinPlaySound && joinSoundKey != null && !joinSoundKey.isEmpty()) {
                try {
                    Sound sound = Sound.sound(Key.key(joinSoundKey), Sound.Source.MASTER, 1.0f, 1.0f);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(sound);
                    }
                } catch (Exception ignored) {
                }
            }
        } else if (player != null && player.isOnline()) {
            // Send privately to the joining player
            for (String rawLine : joinBroadcastItem.messages()) {
                Component lineComp = formatLine(rawLine, joinBroadcastItem.clickValue(), clickEvent, hoverEvent, player);
                player.sendMessage(lineComp);
            }
            if (joinPlaySound && joinSoundKey != null && !joinSoundKey.isEmpty()) {
                try {
                    Sound sound = Sound.sound(Key.key(joinSoundKey), Sound.Source.MASTER, 1.0f, 1.0f);
                    player.playSound(sound);
                } catch (Exception ignored) {
                }
            }
        }
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
        if ("join".equalsIgnoreCase(id.trim()) || "welcome".equalsIgnoreCase(id.trim())) {
            if (joinBroadcastItem != null) {
                Player first = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
                sendJoinBroadcast(first);
                return true;
            }
            return false;
        }

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

        ClickEvent clickEvent = createClickEvent(item.clickAction(), item.clickValue());
        HoverEvent<?> hoverEvent = (item.hoverText() != null && !item.hoverText().isEmpty())
                ? HoverEvent.showText(ColorUtils.toComponent(item.hoverText()))
                : null;

        for (String rawLine : item.messages()) {
            Component lineComponent = formatLine(rawLine, item.clickValue(), clickEvent, hoverEvent, null);
            Bukkit.broadcast(lineComponent);
        }

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

    private Component formatLine(String line, String clickValue, ClickEvent clickEvent, HoverEvent<?> hoverEvent, Player player) {
        // Placeholders
        String processed = line.replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        if (player != null) {
            processed = processed.replace("{player}", player.getName());
        }

        if (clickEvent == null) {
            return ColorUtils.toComponent(processed);
        }

        // 1. Bracket syntax e.g. "[Click here]" -> interactive button
        Matcher matcher = BRACKET_PATTERN.matcher(processed);
        if (matcher.find()) {
            TextComponent.Builder lineBuilder = Component.text();
            int lastEnd = 0;
            matcher.reset();

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();

                if (start > lastEnd) {
                    lineBuilder.append(ColorUtils.toComponent(processed.substring(lastEnd, start)));
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

            if (lastEnd < processed.length()) {
                lineBuilder.append(ColorUtils.toComponent(processed.substring(lastEnd)));
            }
            return lineBuilder.build();
        }

        // 2. {link} or {url} placeholder syntax
        if (processed.contains("{link}") || processed.contains("{url}")) {
            String token = processed.contains("{link}") ? "{link}" : "{url}";
            String[] parts = processed.split(Pattern.quote(token), -1);
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

        return ColorUtils.toComponent(processed);
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

    public boolean isJoinEnabled() {
        return joinEnabled;
    }

    public BroadcastItem getJoinBroadcastItem() {
        return joinBroadcastItem;
    }

    public List<BroadcastItem> getBroadcastList() {
        return broadcastList;
    }
}

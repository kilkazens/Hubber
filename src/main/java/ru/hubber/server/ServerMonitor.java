package ru.hubber.server;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import ru.hubber.HubberPlugin;
import ru.hubber.config.ConfigManager;
import ru.hubber.utils.ColorUtils;

import java.time.Duration;

public class ServerMonitor {

    public enum ServerState {
        UNKNOWN,
        ONLINE,
        RESTARTING
    }

    private final HubberPlugin plugin;
    private final ConfigManager config;

    private ServerState state = ServerState.UNKNOWN;
    private ServerPinger.PingResult lastResult = ServerPinger.PingResult.offline();
    private long restartStartTime = 0;
    private BukkitTask monitorTask;
    private BukkitTask hideBossBarTask;

    private BossBar bossBar;
    private boolean bossBarActive = false;

    public ServerMonitor(HubberPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        start();
    }

    public void start() {
        stop();

        if (!config.isServerMonitorEnabled()) {
            return;
        }

        long intervalTicks = Math.max(1, config.getServerMonitorCheckInterval()) * 20L;

        // Run asynchronous polling loop
        this.monitorTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkServer, 10L, intervalTicks);
    }

    public void stop() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
        if (hideBossBarTask != null) {
            hideBossBarTask.cancel();
            hideBossBarTask = null;
        }
        hideBossBarFromAll();
    }

    public void reload() {
        start();
    }

    private void checkServer() {
        String host = config.getServerMonitorHost();
        int port = config.getServerMonitorPort();
        int timeout = config.getServerMonitorTimeoutMs();

        ServerPinger.PingResult result = ServerPinger.ping(host, port, timeout);
        this.lastResult = result;

        // Sync back to main thread for state transition and Bukkit/Adventure events
        Bukkit.getScheduler().runTask(plugin, () -> handlePingResult(result));
    }

    private void handlePingResult(ServerPinger.PingResult result) {
        if (result.online()) {
            if (state == ServerState.RESTARTING) {
                // Server just finished rebooting!
                onServerOnline(result);
            } else if (state == ServerState.UNKNOWN) {
                // Initial check: server is already online
                state = ServerState.ONLINE;
            }
        } else {
            if (state == ServerState.ONLINE || state == ServerState.UNKNOWN) {
                // Server just went offline / restarted!
                onServerRestarting();
            } else if (state == ServerState.RESTARTING) {
                // Still restarting: update BossBar timer
                updateRestartingBossBar();
            }
        }
    }

    private void onServerRestarting() {
        this.state = ServerState.RESTARTING;
        this.restartStartTime = System.currentTimeMillis();

        if (hideBossBarTask != null) {
            hideBossBarTask.cancel();
            hideBossBarTask = null;
        }

        // 1. Broadcast chat notification
        if (config.isServerMonitorBroadcastChat()) {
            String msg = config.getFormattedMessage("server_monitor.restarting_chat");
            if (!msg.isEmpty()) {
                Bukkit.broadcast(ColorUtils.toComponent(msg));
            }
        }

        // 2. Show BossBar
        if (config.isServerMonitorShowBossBar()) {
            String barText = config.getMessage("server_monitor.restarting_bossbar");
            Component title = ColorUtils.toComponent(barText.replace("{seconds}", "0"));

            if (bossBar == null) {
                bossBar = BossBar.bossBar(title, 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
            } else {
                bossBar.name(title);
                bossBar.color(BossBar.Color.YELLOW);
                bossBar.progress(1.0f);
            }

            bossBarActive = true;
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showBossBar(bossBar);
            }
        }
    }

    private void updateRestartingBossBar() {
        if (!bossBarActive || bossBar == null || !config.isServerMonitorShowBossBar()) {
            return;
        }

        long elapsedSec = (System.currentTimeMillis() - restartStartTime) / 1000;
        String barText = config.getMessage("server_monitor.restarting_bossbar");
        if (barText.contains("{seconds}")) {
            barText = barText.replace("{seconds}", String.valueOf(elapsedSec));
        } else {
            barText = barText + " &7(" + elapsedSec + "s)";
        }

        bossBar.name(ColorUtils.toComponent(barText));
    }

    private void onServerOnline(ServerPinger.PingResult result) {
        this.state = ServerState.ONLINE;
        long elapsedSec = (System.currentTimeMillis() - restartStartTime) / 1000;

        // 1. Play chime sound to all players
        if (config.isServerMonitorPlaySound()) {
            try {
                String soundKey = config.getServerMonitorSound();
                Sound sound = Sound.sound(Key.key(soundKey), Sound.Source.MASTER, 1.0f, 1.0f);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(sound);
                }
            } catch (Exception ignored) {
            }
        }

        // 2. Send Title & Subtitle
        if (config.isServerMonitorSendTitle()) {
            String rawTitle = config.getMessage("server_monitor.online_title");
            String rawSub = config.getMessage("server_monitor.online_subtitle")
                    .replace("{seconds}", String.valueOf(elapsedSec))
                    .replace("{online}", String.valueOf(result.onlinePlayers()));

            Component titleComp = ColorUtils.toComponent(rawTitle);
            Component subComp = ColorUtils.toComponent(rawSub);

            Title title = Title.title(titleComp, subComp,
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3500), Duration.ofMillis(800)));

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(title);
            }
        }

        // 3. Broadcast chat announcement with interactive click-to-connect button
        if (config.isServerMonitorBroadcastChat()) {
            String rawChat = config.getMessage("server_monitor.online_chat")
                    .replace("{seconds}", String.valueOf(elapsedSec))
                    .replace("{online}", String.valueOf(result.onlinePlayers()));
            String rawClick = config.getMessage("server_monitor.online_chat_click");
            String rawHover = config.getMessage("server_monitor.online_chat_hover");

            TextComponent.Builder builder = Component.text();
            builder.append(ColorUtils.toComponent(config.getPrefix() + rawChat + " "));

            if (!rawClick.isEmpty()) {
                Component clickComponent = ColorUtils.toComponent(rawClick)
                        .clickEvent(ClickEvent.runCommand("/joinserver"))
                        .hoverEvent(HoverEvent.showText(ColorUtils.toComponent(rawHover)));
                builder.append(clickComponent);
            }

            Bukkit.broadcast(builder.build());
        }

        // 4. Update BossBar to Green and schedule auto-hide after 7 seconds
        if (bossBarActive && bossBar != null) {
            String onlineBarText = config.getMessage("server_monitor.online_bossbar")
                    .replace("{seconds}", String.valueOf(elapsedSec));
            bossBar.name(ColorUtils.toComponent(onlineBarText));
            bossBar.color(BossBar.Color.GREEN);
            bossBar.progress(1.0f);

            this.hideBossBarTask = Bukkit.getScheduler().runTaskLater(plugin, this::hideBossBarFromAll, 140L);
        }
    }

    private void hideBossBarFromAll() {
        if (bossBar != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.hideBossBar(bossBar);
            }
        }
        bossBarActive = false;
    }

    public void addPlayer(Player player) {
        if (bossBarActive && bossBar != null) {
            player.showBossBar(bossBar);
        }
    }

    public void removePlayer(Player player) {
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    public boolean isOnline() {
        return state == ServerState.ONLINE;
    }

    public boolean isRestarting() {
        return state == ServerState.RESTARTING;
    }

    public ServerState getState() {
        return state;
    }

    public ServerPinger.PingResult getLastResult() {
        return lastResult;
    }

    public boolean isEnabled() {
        return config.isServerMonitorEnabled();
    }
}

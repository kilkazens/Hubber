package ru.hubber.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.hubber.HubberPlugin;
import ru.hubber.config.ConfigManager;

public class JoinServerCommand implements CommandExecutor {

    private final HubberPlugin plugin;
    private final ConfigManager config;

    public JoinServerCommand(HubberPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.getFormattedMessage("only_players"));
            return true;
        }

        if (!player.hasPermission("hubber.joinserver")) {
            player.sendMessage(config.getFormattedMessage("no_permission"));
            return true;
        }

        String targetServer = config.getTargetServer();
        if (args.length > 0 && player.hasPermission("hubber.admin")) {
            targetServer = args[0];
        }

        // Check if main server is currently offline or restarting
        if (config.isServerMonitorBlockJoinWhileOffline()
                && plugin.getServerMonitor() != null
                && plugin.getServerMonitor().isEnabled()
                && !plugin.getServerMonitor().isOnline()
                && !player.hasPermission("hubber.admin")) {
            player.sendMessage(config.getFormattedMessage("server_monitor.restarting_warning"));
            return true;
        }

        // Inform the player
        String connectMsg = config.getFormattedMessage("connecting_to_server").replace("{server}", targetServer);
        player.sendMessage(connectMsg);

        // Send BungeeCord / Velocity plugin message
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(targetServer);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());

        return true;
    }
}

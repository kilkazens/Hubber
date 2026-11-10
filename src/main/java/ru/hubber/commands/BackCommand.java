package ru.hubber.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.hubber.config.ConfigManager;
import ru.hubber.spawn.SpawnManager;

public class BackCommand implements CommandExecutor {

    private final ConfigManager config;
    private final SpawnManager spawnManager;

    public BackCommand(ConfigManager config, SpawnManager spawnManager) {
        this.config = config;
        this.spawnManager = spawnManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.getFormattedMessage("only_players"));
            return true;
        }

        if (!player.hasPermission("hubber.back")) {
            player.sendMessage(config.getFormattedMessage("no_permission"));
            return true;
        }

        spawnManager.teleportToSpawn(player, true);
        return true;
    }
}

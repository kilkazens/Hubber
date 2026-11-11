package ru.hubber.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.hubber.HubberPlugin;
import ru.hubber.book.BookManager;
import ru.hubber.collision.CollisionManager;
import ru.hubber.config.ConfigManager;
import ru.hubber.spawn.SpawnManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HubberCommand implements CommandExecutor, TabCompleter {

    private final HubberPlugin plugin;
    private final ConfigManager config;
    private final SpawnManager spawnManager;
    private final CollisionManager collisionManager;
    private final BookManager bookManager;
    private final ru.hubber.trigger.TriggerManager triggerManager;
    private final ru.hubber.npc.NpcManager npcManager;

    public HubberCommand(HubberPlugin plugin, ConfigManager config, SpawnManager spawnManager,
                         CollisionManager collisionManager, BookManager bookManager,
                         ru.hubber.trigger.TriggerManager triggerManager,
                         ru.hubber.npc.NpcManager npcManager) {
        this.plugin = plugin;
        this.config = config;
        this.spawnManager = spawnManager;
        this.collisionManager = collisionManager;
        this.bookManager = bookManager;
        this.triggerManager = triggerManager;
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("hubber.admin")) {
            sender.sendMessage(config.getFormattedMessage("no_permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(config.getMessage("commands.help_header"));
            sender.sendMessage(config.getMessage("commands.help_reload"));
            sender.sendMessage(config.getMessage("commands.help_setspawn"));
            sender.sendMessage(config.getMessage("commands.help_book"));
            sender.sendMessage(config.getMessage("commands.help_broadcast"));
            sender.sendMessage(config.getMessage("commands.help_joinserver"));
            sender.sendMessage(config.getMessage("commands.help_hubspawn"));
            sender.sendMessage(config.getMessage("commands.help_back"));
            sender.sendMessage(config.getMessage("commands.help_verification"));
            sender.sendMessage(config.getMessage("commands.help_settrigger"));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> {
                config.reload();
                spawnManager.loadSpawn();
                triggerManager.loadTriggers();
                npcManager.loadNpcs();
                if (plugin.getServerMonitor() != null) {
                    plugin.getServerMonitor().reload();
                }
                if (plugin.getBroadcastManager() != null) {
                    plugin.getBroadcastManager().reload();
                }
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    collisionManager.applyCollisionRule(player);
                }
                sender.sendMessage(config.getFormattedMessage("reload_success"));
                return true;
            }
            case "broadcast", "bc" -> {
                if (plugin.getBroadcastManager() == null) {
                    sender.sendMessage(config.getPrefix() + "§cBroadcast manager is not available!");
                    return true;
                }
                if (args.length > 1) {
                    String id = args[1];
                    boolean success = plugin.getBroadcastManager().broadcastById(id);
                    if (success) {
                        sender.sendMessage(config.getPrefix() + "§aBroadcast '§e" + id + "§a' sent successfully!");
                    } else {
                        List<String> ids = new ArrayList<>();
                        if (plugin.getBroadcastManager().getJoinBroadcastItem() != null) {
                            ids.add("join");
                        }
                        ids.addAll(plugin.getBroadcastManager().getBroadcastList().stream().map(ru.hubber.broadcast.BroadcastItem::id).toList());
                        sender.sendMessage(config.getPrefix() + "§cBroadcast '§e" + id + "§c' not found! Available: §e" + String.join(", ", ids));
                    }
                } else {
                    if (plugin.getBroadcastManager().getBroadcastList().isEmpty()) {
                        sender.sendMessage(config.getPrefix() + "§cNo periodic broadcasts configured in broadcasts.yml!");
                        return true;
                    }
                    plugin.getBroadcastManager().broadcastNext();
                    sender.sendMessage(config.getPrefix() + "§aNext broadcast sent to all players!");
                }
                return true;
            }
            case "setspawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(config.getFormattedMessage("only_players"));
                    return true;
                }
                spawnManager.setSpawn(player.getLocation());
                player.sendMessage(config.getFormattedMessage("spawn_set"));
                return true;
            }
            case "book" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(config.getFormattedMessage("only_players"));
                    return true;
                }
                bookManager.giveBook(player);
                player.sendMessage(config.getFormattedMessage("book_given"));
                return true;
            }
            default -> {
                sender.sendMessage(config.getPrefix() + config.getMessage("commands.unknown_subcommand") + label);
                return true;
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("hubber.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (String option : List.of("reload", "setspawn", "book", "broadcast")) {
                if (option.startsWith(prefix)) {
                    completions.add(option);
                }
            }
            return completions;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("broadcast") || args[0].equalsIgnoreCase("bc"))) {
            if (plugin.getBroadcastManager() != null) {
                String prefix = args[1].toLowerCase();
                List<String> ids = new ArrayList<>();
                if ("join".startsWith(prefix)) {
                    ids.add("join");
                }
                for (ru.hubber.broadcast.BroadcastItem item : plugin.getBroadcastManager().getBroadcastList()) {
                    if (item.id().toLowerCase().startsWith(prefix)) {
                        ids.add(item.id());
                    }
                }
                return ids;
            }
        }

        return Collections.emptyList();
    }
}

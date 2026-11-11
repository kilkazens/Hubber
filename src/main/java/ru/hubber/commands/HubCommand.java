package ru.hubber.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.hubber.config.ConfigManager;
import ru.hubber.npc.NpcData;
import ru.hubber.npc.NpcManager;
import ru.hubber.spawn.SpawnManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HubCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager config;
    private final SpawnManager spawnManager;
    private final NpcManager npcManager;

    public HubCommand(ConfigManager config, SpawnManager spawnManager, NpcManager npcManager) {
        this.config = config;
        this.spawnManager = spawnManager;
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Case 1: /hub with no arguments -> teleport to hub spawn
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(config.getFormattedMessage("only_players"));
                return true;
            }
            spawnManager.teleportToSpawn(player, true);
            return true;
        }

        // Case 2: /hub with arguments -> NPC management (requires permission)
        if (!sender.hasPermission("hubber.admin")) {
            sender.sendMessage(config.getFormattedMessage("no_permission"));
            return true;
        }

        // /hub list
        if (args[0].equalsIgnoreCase("list")) {
            var npcs = npcManager.getNpcs();
            if (npcs.isEmpty()) {
                sender.sendMessage(config.getFormattedMessage("npc.list_empty"));
                return true;
            }

            String header = config.getMessage("npc.list_header")
                    .replace("{count}", String.valueOf(npcs.size()));
            sender.sendMessage(header);

            for (NpcData data : npcs) {
                sender.sendMessage(String.format("§8» §e%s §7(Name: %s§7) — World: §f%s§7, [X: §f%.1f§7, Y: §f%.1f§7, Z: §f%.1f§7] → Command: §a/%s",
                        data.getId(), data.getDisplayName(), data.getWorldName(),
                        data.getX(), data.getY(), data.getZ(), data.getCommand()));
            }
            return true;
        }

        // /hub remove [id]
        if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("delete")) {
            if (args.length > 1) {
                String id = args[1];
                if (npcManager.removeNpc(id)) {
                    sender.sendMessage(config.getFormattedMessage("npc.deleted").replace("{id}", id));
                } else {
                    sender.sendMessage(config.getFormattedMessage("npc.not_found").replace("{id}", id));
                }
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage(config.getPrefix() + "§cSpecify NPC ID: /" + label + " remove <id>");
                return true;
            }

            // Remove nearest NPC within 4 blocks
            NpcData nearest = npcManager.removeNearestNpc(player, 4.0);
            if (nearest != null) {
                sender.sendMessage(config.getFormattedMessage("npc.nearest_deleted")
                        .replace("{id}", nearest.getId())
                        .replace("{name}", nearest.getDisplayName()));
            } else {
                sender.sendMessage(config.getFormattedMessage("npc.no_nearby"));
            }
            return true;
        }

        // Syntax: /hub <команда> <ссылка на скин> <Имя> <Цвет имени> [bold] [italic]
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.getFormattedMessage("only_players"));
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(config.getFormattedMessage("npc.usage"));
            return true;
        }

        String npcCmd = args[0].trim();
        if (npcCmd.startsWith("/")) {
            npcCmd = npcCmd.substring(1);
        }

        String skinUrl = args[1].trim();
        String name = args[2].trim();
        String color = args[3].trim();
        boolean bold = args.length > 4 && isTruthy(args[4]);
        boolean italic = args.length > 5 && isTruthy(args[5]);

        NpcData created = npcManager.createNpc(player, npcCmd, skinUrl, name, color, bold, italic);
        if (created != null) {
            sender.sendMessage(config.getFormattedMessage("npc.created")
                    .replace("{name}", created.getDisplayName())
                    .replace("{command}", created.getCommand()));
        } else {
            sender.sendMessage(config.getPrefix() + "§cFailed to create NPC!");
        }

        return true;
    }

    private boolean isTruthy(String s) {
        if (s == null) return false;
        String lower = s.toLowerCase();
        return lower.equals("true") || lower.equals("yes") || lower.equals("1")
                || lower.equals("bold") || lower.equals("italic");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("hubber.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (String option : List.of("joinserver", "verification", "list", "remove")) {
                if (option.startsWith(prefix)) {
                    list.add(option);
                }
            }
            return list;
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("delete")) {
                List<String> list = new ArrayList<>();
                String prefix = args[1].toLowerCase();
                for (NpcData data : npcManager.getNpcs()) {
                    if (data.getId().toLowerCase().startsWith(prefix)) {
                        list.add(data.getId());
                    }
                }
                return list;
            }
            return List.of("default", "https://textures.minecraft.net/texture/<hash>", "Notch");
        }

        if (args.length == 3) {
            return List.of("Server", "Survival", "Main");
        }

        if (args.length == 4) {
            List<String> colors = List.of("gold", "yellow", "green", "red", "aqua", "blue", "white", "light_purple", "gray", "#FFD700");
            List<String> list = new ArrayList<>();
            String prefix = args[3].toLowerCase();
            for (String col : colors) {
                if (col.startsWith(prefix)) {
                    list.add(col);
                }
            }
            return list;
        }

        if (args.length == 5) {
            return List.of("true", "false", "bold");
        }

        if (args.length == 6) {
            return List.of("true", "false", "italic");
        }

        return Collections.emptyList();
    }
}

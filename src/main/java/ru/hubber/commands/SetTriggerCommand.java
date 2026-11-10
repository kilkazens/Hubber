package ru.hubber.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.hubber.config.ConfigManager;
import ru.hubber.trigger.Trigger;
import ru.hubber.trigger.TriggerManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SetTriggerCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager config;
    private final TriggerManager triggerManager;

    public SetTriggerCommand(ConfigManager config, TriggerManager triggerManager) {
        this.config = config;
        this.triggerManager = triggerManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("hubber.settrigger")) {
            sender.sendMessage(config.getFormattedMessage("no_permission"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            var triggers = triggerManager.getTriggers();
            if (triggers.isEmpty()) {
                sender.sendMessage(config.getFormattedMessage("triggers.list_empty"));
                return true;
            }

            String header = config.getMessage("triggers.list_header")
                    .replace("{count}", String.valueOf(triggers.size()));
            sender.sendMessage(header);

            for (Trigger trig : triggers) {
                if (trig.getType() == Trigger.Type.SPHERE) {
                    sender.sendMessage(String.format("§8» §e%s §7(Sphere) — World: §f%s§7, X: §f%.1f§7, Y: §f%.1f§7, Z: §f%.1f§7, Radius: §a%.1f§7m",
                            trig.getId(), trig.getWorldName(), trig.getX(), trig.getY(), trig.getZ(), trig.getRadius()));
                } else {
                    sender.sendMessage(String.format("§8» §e%s §7(Cuboid) — World: §f%s§7, [X: §f%.1f §7to §f%.1f§7, Y: §f%.1f §7to §f%.1f§7, Z: §f%.1f §7to §f%.1f§7]",
                            trig.getId(), trig.getWorldName(), trig.getMinX(), trig.getMaxX(), trig.getMinY(), trig.getMaxY(), trig.getMinZ(), trig.getMaxZ()));
                }
            }
            return true;
        }

        if (args.length > 1 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("delete"))) {
            String name = args[1];
            if (triggerManager.removeTrigger(name)) {
                sender.sendMessage(config.getFormattedMessage("triggers.deleted").replace("{name}", name));
            } else {
                sender.sendMessage(config.getFormattedMessage("triggers.not_found").replace("{name}", name));
            }
            return true;
        }

        // All other actions require a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.getFormattedMessage("only_players"));
            return true;
        }

        // /settrigger pos1
        if (args.length == 1 && args[0].equalsIgnoreCase("pos1")) {
            Location loc = player.getLocation();
            triggerManager.setPos1(player.getUniqueId(), loc);
            sender.sendMessage(config.getFormattedMessage("triggers.pos1_set")
                    .replace("{x}", String.format("%.1f", loc.getX()))
                    .replace("{y}", String.format("%.1f", loc.getY()))
                    .replace("{z}", String.format("%.1f", loc.getZ())));
            return true;
        }

        // /settrigger pos2
        if (args.length == 1 && args[0].equalsIgnoreCase("pos2")) {
            Location loc = player.getLocation();
            triggerManager.setPos2(player.getUniqueId(), loc);
            sender.sendMessage(config.getFormattedMessage("triggers.pos2_set")
                    .replace("{x}", String.format("%.1f", loc.getX()))
                    .replace("{y}", String.format("%.1f", loc.getY()))
                    .replace("{z}", String.format("%.1f", loc.getZ())));
            return true;
        }

        // /settrigger create <name> (cuboid)
        if (args.length >= 2 && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("box"))) {
            String name = args[1];
            Location p1 = triggerManager.getPos1(player.getUniqueId());
            Location p2 = triggerManager.getPos2(player.getUniqueId());

            if (p1 == null || p2 == null) {
                sender.sendMessage(config.getFormattedMessage("triggers.need_positions"));
                return true;
            }

            if (!p1.getWorld().equals(p2.getWorld())) {
                sender.sendMessage(config.getFormattedMessage("triggers.diff_worlds"));
                return true;
            }

            triggerManager.addCuboidTrigger(name, p1, p2);
            triggerManager.clearSelection(player.getUniqueId());
            sender.sendMessage(config.getFormattedMessage("triggers.cuboid_created").replace("{name}", name));
            return true;
        }

        // Sphere triggers:
        // Case 1: /settrigger (default radius, auto-generated name)
        // Case 2: /settrigger <radius>
        // Case 3: /settrigger <name>
        // Case 4: /settrigger <name> <radius>

        double radius = config.getTriggerDefaultRadius();
        String name;

        if (args.length == 0) {
            name = generateAutoName();
        } else if (args.length == 1) {
            try {
                radius = Double.parseDouble(args[0]);
                name = generateAutoName();
            } catch (NumberFormatException e) {
                name = args[0];
            }
        } else {
            name = args[0];
            try {
                radius = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(config.getPrefix() + "§cRadius must be a number! Usage: /" + label + " [name] [radius]");
                return true;
            }
        }

        if (radius <= 0) {
            sender.sendMessage(config.getPrefix() + "§cRadius must be greater than 0!");
            return true;
        }

        triggerManager.addSphereTrigger(name, player.getLocation(), radius);
        sender.sendMessage(config.getFormattedMessage("triggers.created")
                .replace("{name}", name)
                .replace("{radius}", String.format("%.1f", radius)));
        return true;
    }

    private String generateAutoName() {
        int index = 1;
        while (triggerManager.getTrigger("trig_" + index) != null) {
            index++;
        }
        return "trig_" + index;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("hubber.settrigger")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (String sub : List.of("pos1", "pos2", "create", "list", "remove", "1.5", "2.0", "3.0", "5.0")) {
                if (sub.startsWith(prefix)) {
                    list.add(sub);
                }
            }
            return list;
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("delete")) {
                List<String> list = new ArrayList<>();
                String prefix = args[1].toLowerCase();
                for (Trigger trig : triggerManager.getTriggers()) {
                    if (trig.getId().startsWith(prefix)) {
                        list.add(trig.getId());
                    }
                }
                return list;
            }
            if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("box")) {
                return List.of("<имя_зоны>");
            }
            return List.of("1.5", "2.0", "3.0", "5.0");
        }

        return Collections.emptyList();
    }
}

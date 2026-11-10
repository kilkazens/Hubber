package ru.hubber.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.hubber.config.ConfigManager;
import ru.hubber.utils.ColorUtils;

import java.util.List;

public class VerificationCommand implements CommandExecutor {

    private final ConfigManager config;

    public VerificationCommand(ConfigManager config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("hubber.verification")) {
            sender.sendMessage(config.getFormattedMessage("no_permission"));
            return true;
        }

        List<String> messageLines = config.getVerificationMessage();
        String link = config.getVerificationLink();
        String hoverText = config.getVerificationHoverText();

        if (sender instanceof Player player) {
            ColorUtils.sendVerificationMessage(player, messageLines, link, hoverText);
        } else {
            for (String line : messageLines) {
                sender.sendMessage(ColorUtils.colorize(line.replace("{link}", link)));
            }
        }

        return true;
    }
}

package ru.hubber.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern BRACKET_HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private ColorUtils() {
    }

    /**
     * Translates '&' color codes and hex codes (&#RRGGBB and <#RRGGBB>) into Adventure Component.
     */
    public static Component toComponent(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_SERIALIZER.deserialize(normalizeHex(message));
    }

    /**
     * Translates a list of strings into a list of Components.
     */
    public static List<Component> toComponentList(List<String> lines) {
        if (lines == null) {
            return new ArrayList<>();
        }
        List<Component> list = new ArrayList<>(lines.size());
        for (String line : lines) {
            list.add(toComponent(line));
        }
        return list;
    }

    /**
     * Legacy string colorizer (supports &#RRGGBB, <#RRGGBB>, and & codes).
     */
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        return LegacyComponentSerializer.legacySection().serialize(toComponent(message));
    }

    /**
     * Parses a color string (named, hex, or color code) into an Adventure TextColor.
     */
    public static net.kyori.adventure.text.format.TextColor parseTextColor(String input) {
        if (input == null || input.trim().isEmpty()) {
            return net.kyori.adventure.text.format.NamedTextColor.WHITE;
        }

        String clean = input.trim().toLowerCase();
        net.kyori.adventure.text.format.NamedTextColor named =
                net.kyori.adventure.text.format.NamedTextColor.NAMES.value(clean.replace('-', '_'));
        if (named != null) {
            return named;
        }

        if (clean.startsWith("&") && clean.length() == 2) {
            char code = clean.charAt(1);
            return switch (code) {
                case '0' -> net.kyori.adventure.text.format.NamedTextColor.BLACK;
                case '1' -> net.kyori.adventure.text.format.NamedTextColor.DARK_BLUE;
                case '2' -> net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN;
                case '3' -> net.kyori.adventure.text.format.NamedTextColor.DARK_AQUA;
                case '4' -> net.kyori.adventure.text.format.NamedTextColor.DARK_RED;
                case '5' -> net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE;
                case '6' -> net.kyori.adventure.text.format.NamedTextColor.GOLD;
                case '7' -> net.kyori.adventure.text.format.NamedTextColor.GRAY;
                case '8' -> net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
                case '9' -> net.kyori.adventure.text.format.NamedTextColor.BLUE;
                case 'a' -> net.kyori.adventure.text.format.NamedTextColor.GREEN;
                case 'b' -> net.kyori.adventure.text.format.NamedTextColor.AQUA;
                case 'c' -> net.kyori.adventure.text.format.NamedTextColor.RED;
                case 'd' -> net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
                case 'e' -> net.kyori.adventure.text.format.NamedTextColor.YELLOW;
                default -> net.kyori.adventure.text.format.NamedTextColor.WHITE;
            };
        }

        if (clean.startsWith("&#")) {
            clean = clean.substring(1);
        } else if (clean.startsWith("<#") && clean.endsWith(">")) {
            clean = clean.substring(1, clean.length() - 1);
        }

        if (!clean.startsWith("#") && (clean.length() == 6 || clean.length() == 8)) {
            clean = "#" + clean;
        }

        net.kyori.adventure.text.format.TextColor hex = net.kyori.adventure.text.format.TextColor.fromHexString(clean);
        return hex != null ? hex : net.kyori.adventure.text.format.NamedTextColor.WHITE;
    }

    /**
     * Translates a list of strings with color codes into legacy strings.
     */
    public static List<String> colorize(List<String> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>(list.size());
        for (String line : list) {
            result.add(colorize(line));
        }
        return result;
    }

    private static String normalizeHex(String message) {
        Matcher bracketMatcher = BRACKET_HEX_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (bracketMatcher.find()) {
            bracketMatcher.appendReplacement(sb, "&#" + bracketMatcher.group(1));
        }
        bracketMatcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Sends the verification message to a player using Paper Adventure Components.
     * The placeholder {link} becomes a clickable URL with hover tooltip.
     */
    public static void sendVerificationMessage(Player player, List<String> lines, String url, String hoverText) {
        if (lines == null || lines.isEmpty()) {
            return;
        }

        Component hoverComponent = toComponent(hoverText);

        for (String rawLine : lines) {
            if (rawLine.contains("{link}")) {
                String[] parts = rawLine.split(Pattern.quote("{link}"), -1);
                TextComponent.Builder lineBuilder = Component.text();

                for (int i = 0; i < parts.length; i++) {
                    if (!parts[i].isEmpty()) {
                        lineBuilder.append(toComponent(parts[i]));
                    }
                    if (i < parts.length - 1) {
                        Component linkComponent = toComponent(url)
                                .clickEvent(ClickEvent.openUrl(url))
                                .hoverEvent(HoverEvent.showText(hoverComponent));
                        lineBuilder.append(linkComponent);
                    }
                }
                player.sendMessage(lineBuilder.build());
            } else {
                player.sendMessage(toComponent(rawLine));
            }
        }
    }
}

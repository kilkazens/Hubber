package ru.hubber.broadcast;

import java.util.List;

public record BroadcastItem(
        String id,
        List<String> messages,
        String clickAction,
        String clickValue,
        String hoverText
) {
}

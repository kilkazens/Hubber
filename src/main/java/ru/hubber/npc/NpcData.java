package ru.hubber.npc;

import java.util.UUID;

public class NpcData {

    private final String id;
    private final UUID entityUuid;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final String command;
    private final String skinInput;
    private final String displayName;
    private final String color;
    private final boolean bold;
    private final boolean italic;

    public NpcData(String id, UUID entityUuid, String worldName, double x, double y, double z,
                   float yaw, float pitch, String command, String skinInput,
                   String displayName, String color, boolean bold, boolean italic) {
        this.id = id;
        this.entityUuid = entityUuid;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.command = command;
        this.skinInput = skinInput;
        this.displayName = displayName;
        this.color = color;
        this.bold = bold;
        this.italic = italic;
    }

    public String getId() {
        return id;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public String getCommand() {
        return command;
    }

    public String getSkinInput() {
        return skinInput;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }
}

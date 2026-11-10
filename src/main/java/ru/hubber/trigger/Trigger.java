package ru.hubber.trigger;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class Trigger {

    public enum Type {
        SPHERE,
        CUBOID
    }

    private final String id;
    private final String worldName;
    private final Type type;

    // Sphere parameters
    private final double x;
    private final double y;
    private final double z;
    private final double radius;
    private final double radiusSquared;

    // Cuboid parameters
    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;

    // Constructor for SPHERE (point with radius)
    public Trigger(String id, String worldName, double x, double y, double z, double radius) {
        this.id = id;
        this.worldName = worldName;
        this.type = Type.SPHERE;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.radiusSquared = radius * radius;

        this.minX = 0;
        this.minY = 0;
        this.minZ = 0;
        this.maxX = 0;
        this.maxY = 0;
        this.maxZ = 0;
    }

    // Constructor for CUBOID (box between two points)
    public Trigger(String id, String worldName, double x1, double y1, double z1, double x2, double y2, double z2) {
        this.id = id;
        this.worldName = worldName;
        this.type = Type.CUBOID;

        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);

        this.x = (this.minX + this.maxX) / 2.0;
        this.y = (this.minY + this.maxY) / 2.0;
        this.z = (this.minZ + this.maxZ) / 2.0;
        this.radius = 0;
        this.radiusSquared = 0;
    }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }

        if (!loc.getWorld().getName().equals(worldName)) {
            return false;
        }

        if (type == Type.SPHERE) {
            double dx = loc.getX() - x;
            double dy = loc.getY() - y;
            double dz = loc.getZ() - z;
            return (dx * dx + dy * dy + dz * dz) <= radiusSquared;
        } else {
            double px = loc.getX();
            double py = loc.getY();
            double pz = loc.getZ();
            return px >= minX && px <= maxX
                    && py >= minY && py <= maxY
                    && pz >= minZ && pz <= maxZ;
        }
    }

    public void saveToSection(ConfigurationSection section) {
        section.set("type", type.name());
        section.set("world", worldName);
        if (type == Type.SPHERE) {
            section.set("x", x);
            section.set("y", y);
            section.set("z", z);
            section.set("radius", radius);
        } else {
            section.set("minX", minX);
            section.set("minY", minY);
            section.set("minZ", minZ);
            section.set("maxX", maxX);
            section.set("maxY", maxY);
            section.set("maxZ", maxZ);
        }
    }

    public static Trigger loadFromSection(String id, ConfigurationSection section) {
        String world = section.getString("world", "world");
        String typeStr = section.getString("type", "SPHERE");
        Type type = Type.valueOf(typeStr.toUpperCase());

        if (type == Type.SPHERE) {
            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            double radius = section.getDouble("radius", 2.0);
            return new Trigger(id, world, x, y, z, radius);
        } else {
            double minX = section.getDouble("minX");
            double minY = section.getDouble("minY");
            double minZ = section.getDouble("minZ");
            double maxX = section.getDouble("maxX");
            double maxY = section.getDouble("maxY");
            double maxZ = section.getDouble("maxZ");
            return new Trigger(id, world, minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public String getId() {
        return id;
    }

    public String getWorldName() {
        return worldName;
    }

    public Type getType() {
        return type;
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

    public double getRadius() {
        return radius;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMinZ() {
        return minZ;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public double getMaxZ() {
        return maxZ;
    }
}

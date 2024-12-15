package org.teacon.signmeup.config.waypoints;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public record Waypoint(String name, String description, int x, int y, int z, float rx, float ry) {
    public static final Map<String, Waypoint> INSTANCES = new ConcurrentHashMap<>();

    public static final StreamCodec<ByteBuf, Waypoint> CODEC = NeoForgeStreamCodecs.composite(
            ByteBufCodecs.STRING_UTF8, Waypoint::name,
            ByteBufCodecs.STRING_UTF8, Waypoint::description,
            ByteBufCodecs.VAR_INT, Waypoint::x,
            ByteBufCodecs.VAR_INT, Waypoint::y,
            ByteBufCodecs.VAR_INT, Waypoint::z,
            ByteBufCodecs.FLOAT, Waypoint::rx,
            ByteBufCodecs.FLOAT, Waypoint::ry,
            Waypoint::new
    );

    public static void load() {
        for (Waypoint waypoint : WaypointSavedData.load()) {
            INSTANCES.put(waypoint.name, waypoint);
        }
    }

    public static void save() {
        WaypointSavedData.save(INSTANCES.values());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Waypoint that) {
            return Objects.equals(that.name, name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "[WayPoint name=" + name + ", description=" + description + ", <" + x + ", " + y + ", " + z + ">, rotation=<" + rx + ", " + ry + ">]";
    }
}

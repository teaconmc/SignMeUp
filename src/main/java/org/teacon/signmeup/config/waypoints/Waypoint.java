package org.teacon.signmeup.config.waypoints;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Waypoint {
    public static final Map<String, Waypoint> INSTANCES = new ConcurrentHashMap<>();

    public static final StreamCodec<ByteBuf, Waypoint> CODEC = new StreamCodec<>() {
        @Override
        public @NotNull Waypoint decode(@NotNull ByteBuf buffer) {
            return new Waypoint(
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.FLOAT.decode(buffer),
                    ByteBufCodecs.FLOAT.decode(buffer)
            );
        }

        @Override
        public void encode(@NotNull ByteBuf buffer, @NotNull Waypoint value) {
            ByteBufCodecs.STRING_UTF8.encode(buffer, value.name);
            ByteBufCodecs.STRING_UTF8.encode(buffer, value.description);
            ByteBufCodecs.VAR_INT.encode(buffer, value.x);
            ByteBufCodecs.VAR_INT.encode(buffer, value.y);
            ByteBufCodecs.VAR_INT.encode(buffer, value.z);
            ByteBufCodecs.FLOAT.encode(buffer, value.rx);
            ByteBufCodecs.FLOAT.encode(buffer, value.ry);
        }
    };

    public static void load() {
        for (Waypoint waypoint : WaypointSavedData.load()) {
            INSTANCES.put(waypoint.name, waypoint);
        }
    }

    public static void save() {
        WaypointSavedData.save(INSTANCES.values());
    }

    public final String name, description;
    public final int x, y, z;

    public final float rx, ry;

    public Waypoint(String name, String description, int x, int y, int z, float rx, float ry) {
        this.name = name;
        this.description = description;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rx = rx;
        this.ry = ry;
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

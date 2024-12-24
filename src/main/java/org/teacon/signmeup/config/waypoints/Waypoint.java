package org.teacon.signmeup.config.waypoints;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record Waypoint(UUID uuid, String name, String description, Vector3f pos, Vector2f rotation, State state) {
    public record State(boolean major) {
    }

    public static final Map<UUID, Waypoint> INSTANCES = new ConcurrentHashMap<>();

    public static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, UUID::getMostSignificantBits,
            ByteBufCodecs.VAR_LONG, UUID::getLeastSignificantBits,
            UUID::new
    );

    public static final StreamCodec<ByteBuf, Waypoint> CODEC = StreamCodec.composite(
            UUID_CODEC, Waypoint::uuid,
            ByteBufCodecs.STRING_UTF8, Waypoint::name,
            ByteBufCodecs.STRING_UTF8, Waypoint::description,
            ByteBufCodecs.VECTOR3F, Waypoint::pos,
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, Vector2f::x,
                    ByteBufCodecs.FLOAT, Vector2f::y,
                    Vector2f::new
            ), Waypoint::rotation,
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, State::major,
                    State::new
            ), Waypoint::state,
            Waypoint::new
    );

    public static void load() {
        for (Waypoint waypoint : WaypointSavedData.load()) {
            INSTANCES.put(waypoint.uuid, waypoint);
        }
    }

    public static void save() {
        WaypointSavedData.save(INSTANCES.values());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Waypoint that) {
            return Objects.equals(that.uuid, uuid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "[WayPoint uuid=" + uuid + ", name=" + name + ", description=" + description + ", <" + pos.x + ", " + pos.y + ", " + pos.z + ">, rotation=<" + rotation.x + ", " + rotation.y + ">]";
    }
}

package org.teacon.signmeup.config.waypoints;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.teacon.signmeup.SignMeUp;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class WaypointSavedData extends SavedData {
    private static final Factory<WaypointSavedData> FACTORY = new Factory<>(
            () -> new WaypointSavedData(List.of()),
            (tag, provider) -> new WaypointSavedData(DataReader.read(tag))
    );

    private List<Waypoint> POINT;

    public WaypointSavedData(List<Waypoint> POINT) {
        this.POINT = POINT;
    }

    public static Collection<Waypoint> load() {
        WaypointSavedData instance = getInstance();
        return instance == null ? Collections.emptyList() : instance.POINT;
    }

    public static void save(Collection<Waypoint> values) {
        WaypointSavedData data = getInstance();
        if (data == null) return;
        data.POINT = List.copyOf(values);
        data.setDirty();
    }

    private static @Nullable WaypointSavedData getInstance() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }

        return Objects.requireNonNull(server.getLevel(Level.OVERWORLD), "Level 'overworld' should exist.").getDataStorage()
                .computeIfAbsent(WaypointSavedData.FACTORY, SignMeUp.MODID + "_waypoints");
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        tag.putInt("version", DataReader.ID.version);
        tag.put("data", DataReader.write(POINT));

        return tag;
    }

    public enum DataReader {
        INIT(0) {
            @Override
            protected List<Waypoint> load(Tag tag0) {
                CompoundTag tag = (CompoundTag) tag0;

                Map<UUID, Waypoint> waypoints = new HashMap<>();
                for (String name : tag.getAllKeys()) {
                    CompoundTag point = tag.getCompound(name);
                    CompoundTag rotation = point.getCompound("rotation");
                    int[] pos = point.getIntArray("pos");

                    UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
                    if (waypoints.containsKey(uuid)) {
                        uuid = UUID.randomUUID();
                    }

                    boolean major = name.startsWith("#");
                    if (major) {
                        name = name.substring(1);
                    }

                    waypoints.put(uuid, new Waypoint(
                            uuid, name, point.getString("description"),
                            new Vector3f(pos[0], pos[1], pos[2]), new Vector2f(rotation.getFloat("x"), rotation.getFloat("y")),
                            new Waypoint.State(major)
                    ));
                }

                return new ArrayList<>(waypoints.values());
            }
        }, ID(1) {
            @Override
            protected List<Waypoint> load(Tag tag0) {
                ListTag tag = (ListTag) tag0;

                List<Waypoint> waypoints = new ArrayList<>();
                for (Tag value : tag) {
                    CompoundTag item = (CompoundTag) value;

                    UUID uuid = UUID.fromString(item.getString("uuid"));
                    CompoundTag pos = item.getCompound("pos");
                    CompoundTag rotation = item.getCompound("rotation");
                    CompoundTag state = item.getCompound("state");
                    waypoints.add(new Waypoint(
                            uuid, item.getString("name"), item.getString("description"),
                            new Vector3f(pos.getFloat("x"), pos.getFloat("y"), pos.getFloat("z")),
                            new Vector2f(rotation.getFloat("x"), rotation.getFloat("y")),
                            new Waypoint.State(state.getBoolean("major"))
                    ));
                }
                return waypoints;
            }
        };

        private final int version;

        DataReader(int version) {
            this.version = version;
        }

        protected abstract List<Waypoint> load(Tag tag);

        public static List<Waypoint> read(CompoundTag tag) {
            int v = tag.getInt("version");
            if (v == 0) {
                return INIT.load(tag);
            }

            for (DataReader reader : DataReader.values()) {
                if (reader.version == v) {
                    return reader.load(tag.get("data"));
                }
            }

            return List.of();
        }

        public static Tag write(List<Waypoint> waypoints) {
            ListTag tag = new ListTag();
            for (Waypoint waypoint : waypoints) {
                CompoundTag item = new CompoundTag();
                item.putString("uuid", waypoint.uuid().toString());
                item.putString("name", waypoint.name());
                item.putString("description", waypoint.description());

                CompoundTag pos = new CompoundTag();
                {
                    pos.putFloat("x", waypoint.pos().x);
                    pos.putFloat("y", waypoint.pos().y);
                    pos.putFloat("z", waypoint.pos().z);
                }
                item.put("pos", pos);

                CompoundTag rotation = new CompoundTag();
                {
                    rotation.putFloat("x", waypoint.rotation().x);
                    rotation.putFloat("y", waypoint.rotation().y);
                }
                item.put("rotation", rotation);

                CompoundTag state = new CompoundTag();
                {
                    state.putBoolean("major", waypoint.state().major());
                }
                item.put("state", state);

                tag.add(item);
            }

            return tag;
        }
    }
}

package org.teacon.signmeup.config.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teacon.signmeup.SignMeUp;

import java.util.*;

public class WaypointSavedData extends SavedData {
    private static final Factory<WaypointSavedData> FACTORY = new Factory<>(WaypointSavedData::new, WaypointSavedData::load);

    private List<Waypoint> POINT = new ArrayList<>();

    private static WaypointSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        WaypointSavedData data = new WaypointSavedData();
        for (String name : tag.getAllKeys()) {
            CompoundTag point = tag.getCompound(name);
            CompoundTag rotation = point.getCompound("rotation");
            int[] pos = point.getIntArray("pos");

            data.POINT.add(new Waypoint(
                    name, point.getString("description"),
                    pos[0], pos[1], pos[2], rotation.getFloat("x"), rotation.getFloat("y")
            ));
        }

        data.POINT = Collections.unmodifiableList(data.POINT);
        return data;
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
        MinecraftServer server = switch (FMLEnvironment.dist) {
            case CLIENT -> Minecraft.getInstance().getSingleplayerServer();
            case DEDICATED_SERVER -> ServerLifecycleHooks.getCurrentServer();
        };
        if (server == null) {
            return null;
        }

        WaypointSavedData data = Objects.requireNonNull(server.getLevel(Level.OVERWORLD), "Level 'overworld' should exist.").getDataStorage()
                .computeIfAbsent(WaypointSavedData.FACTORY, SignMeUp.MODID + "_waypoints");
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        for (Waypoint waypoint : Waypoint.INSTANCES.values()) {
            CompoundTag point = new CompoundTag();
            {
                point.putString("description", waypoint.description);
                point.putIntArray("pos", new int[]{waypoint.x, waypoint.y, waypoint.z});
                CompoundTag rotation = new CompoundTag();
                {
                    rotation.putFloat("x", waypoint.rx);
                    rotation.putFloat("y", waypoint.ry);
                }
                point.put("rotation", rotation);
            }
            tag.put(waypoint.name, point);
        }

        return tag;
    }
}

package org.teacon.signin.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.signin.client.GuideMapScreen;
import org.teacon.signin.client.SignMeUpClient;
import org.teacon.signin.data.*;
import org.teacon.signin.data.entity.Trigger;
import org.teacon.signin.data.entity.Waypoint;

import java.util.function.Supplier;

public final class PartialUpdatePacket {

    public enum Mode {
        ADD_WAYPOINT, REMOVE_WAYPOINT, ADD_TRIGGER, REMOVE_TRIGGER
    }

    private static final Gson GSON = new GsonBuilder().setLenient()
            .registerTypeAdapter(Waypoint.class, new Waypoint.Serializer())
            .registerTypeAdapter(Waypoint.Location.class, new Waypoint.Location.Serializer())
            .registerTypeAdapter(Vec3i.class, new Vector3iAdapter())
            .registerTypeAdapter(Trigger.class, new Trigger.Serializer())
            .registerTypeHierarchyAdapter(Component.class, new Component.Serializer())
            .create();

    private Mode mode;
    private ResourceLocation waypointId;
    private Waypoint waypoint;
    private ResourceLocation triggerId;
    private Trigger trigger;

    public PartialUpdatePacket(FriendlyByteBuf buf) {
        switch (this.mode = buf.readEnum(Mode.class)) {
            case ADD_WAYPOINT:
                this.waypointId = buf.readResourceLocation();
                this.waypoint = GSON.fromJson(buf.readUtf(Short.MAX_VALUE), Waypoint.class);
                break;
            case REMOVE_WAYPOINT:
                this.waypointId = buf.readResourceLocation();
                break;
            case ADD_TRIGGER:
                this.triggerId = buf.readResourceLocation();
                this.trigger = GSON.fromJson(buf.readUtf(Short.MAX_VALUE), Trigger.class);
                break;
            case REMOVE_TRIGGER:
                this.triggerId = buf.readResourceLocation();
        }
    }

    public PartialUpdatePacket(Mode mode, ResourceLocation id, Waypoint wp) {
        this.mode = mode;
        this.waypointId = id;
        this.waypoint = wp;
    }

    public PartialUpdatePacket(Mode mode, ResourceLocation id, Trigger trigger) {
        this.mode = mode;
        this.triggerId = id;
        this.trigger = trigger;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.mode);
        switch (this.mode) {
            case REMOVE_WAYPOINT:
                buf.writeResourceLocation(this.waypointId);
                break;
            case ADD_WAYPOINT:
                buf.writeResourceLocation(this.waypointId);
                buf.writeUtf(GSON.toJson(this.waypoint));
                break;
            case REMOVE_TRIGGER:
                buf.writeResourceLocation(this.triggerId);
                break;
            case ADD_TRIGGER:
                buf.writeResourceLocation(this.triggerId);
                buf.writeUtf(GSON.toJson(this.trigger));
                break;
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextGetter) {
        contextGetter.get().enqueueWork(() -> {
            switch (this.mode) {
                case ADD_WAYPOINT:
                    SignMeUpClient.MANAGER.addWaypoint(this.waypointId, this.waypoint);
                    break;
                case REMOVE_WAYPOINT:
                    SignMeUpClient.MANAGER.removeWaypoint(this.waypointId);
                    break;
                case ADD_TRIGGER:
                    SignMeUpClient.MANAGER.addTrigger(this.triggerId, this.trigger);
                    break;
                case REMOVE_TRIGGER:
                    SignMeUpClient.MANAGER.removeTrigger(this.triggerId);
                    break;
            }
            final Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof GuideMapScreen) {
                ((GuideMapScreen) mc.screen).refresh();
            }
        });
        contextGetter.get().setPacketHandled(true);
    }
}

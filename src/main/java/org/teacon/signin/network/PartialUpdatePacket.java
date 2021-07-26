package org.teacon.signin.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import org.teacon.signin.client.SignMeUpClient;
import org.teacon.signin.data.*;

import java.util.function.Supplier;

public final class PartialUpdatePacket {

    public enum Mode {
        ADD_WAYPOINT, REMOVE_WAYPOINT, ADD_TRIGGER, REMOVE_TRIGGER
    }

    private static final Gson GSON = new GsonBuilder().setLenient()
            .registerTypeAdapter(Waypoint.class, new Waypoint.Serializer())
            .registerTypeAdapter(Waypoint.Location.class, new Waypoint.Location.Serializer())
            .registerTypeAdapter(Vector3i.class, new Vector3iAdapter())
            .registerTypeAdapter(Trigger.class, new Trigger.Serializer())
            .registerTypeHierarchyAdapter(ITextComponent.class, new ITextComponent.Serializer())
            .create();

    private Mode mode;
    private ResourceLocation waypointId;
    private Waypoint waypoint;
    private ResourceLocation triggerId;
    private Trigger trigger;

    public PartialUpdatePacket(PacketBuffer buf) {
        switch (this.mode = buf.readEnumValue(Mode.class)) {
            case ADD_WAYPOINT:
                this.waypointId = buf.readResourceLocation();
                this.waypoint = GSON.fromJson(buf.readString(), Waypoint.class);
                break;
            case REMOVE_WAYPOINT:
                this.waypointId = buf.readResourceLocation();
                break;
            case ADD_TRIGGER:
                this.triggerId = buf.readResourceLocation();
                this.trigger = GSON.fromJson(buf.readString(), Trigger.class);
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

    public void write(PacketBuffer buf) {
        buf.writeEnumValue(this.mode);
        switch (this.mode) {
            case REMOVE_WAYPOINT:
                buf.writeResourceLocation(this.waypointId);
                break;
            case ADD_WAYPOINT:
                buf.writeResourceLocation(this.waypointId);
                buf.writeString(GSON.toJson(this.waypoint));
                break;
            case REMOVE_TRIGGER:
                buf.writeResourceLocation(this.triggerId);
                break;
            case ADD_TRIGGER:
                buf.writeResourceLocation(this.triggerId);
                buf.writeString(GSON.toJson(this.trigger));
                break;
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextGetter) {
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
        contextGetter.get().setPacketHandled(true);
    }
}

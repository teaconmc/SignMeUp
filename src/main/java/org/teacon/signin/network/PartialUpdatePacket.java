package org.teacon.signin.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.signin.client.GuideMapScreen;
import org.teacon.signin.client.SignMeUpClient;
import org.teacon.signin.data.Vector3iAdapter;
import org.teacon.signin.data.entity.Trigger;
import org.teacon.signin.data.entity.Waypoint;

import java.util.UUID;
import java.util.function.Supplier;

public final class PartialUpdatePacket {

    public enum Mode {
        ADD_WAYPOINT, REMOVE_WAYPOINT, ADD_TRIGGER, REMOVE_TRIGGER, UPDATE_POSITION, REMOVE_POSITION
    }

    private static final Gson GSON = new GsonBuilder().setLenient()
            .registerTypeAdapter(Waypoint.class, new Waypoint.Serializer())
            .registerTypeAdapter(Waypoint.Location.class, new Waypoint.Location.Serializer())
            .registerTypeAdapter(Vec3i.class, new Vector3iAdapter())
            .registerTypeAdapter(Trigger.class, new Trigger.Serializer())
            .registerTypeHierarchyAdapter(Component.class, new Component.Serializer())
            .create();

    private final Mode mode;
    private ResourceLocation waypointId;
    private Waypoint waypoint;
    private ResourceLocation triggerId;
    private Trigger trigger;
    private GameProfile playerUniqueId;
    private GlobalPos playerPos;

    public PartialUpdatePacket(FriendlyByteBuf buf) {
        switch (this.mode = buf.readEnum(Mode.class)) {
            case ADD_WAYPOINT -> {
                this.waypointId = buf.readResourceLocation();
                this.waypoint = GSON.fromJson(buf.readUtf(Short.MAX_VALUE), Waypoint.class);
            }
            case REMOVE_WAYPOINT -> this.waypointId = buf.readResourceLocation();
            case ADD_TRIGGER -> {
                this.triggerId = buf.readResourceLocation();
                this.trigger = GSON.fromJson(buf.readUtf(Short.MAX_VALUE), Trigger.class);
            }
            case REMOVE_TRIGGER -> this.triggerId = buf.readResourceLocation();
            case UPDATE_POSITION -> {
                this.playerUniqueId = buf.readGameProfile();
                this.playerPos = buf.readGlobalPos();
            }
            case REMOVE_POSITION -> this.playerUniqueId = buf.readGameProfile();
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

    public PartialUpdatePacket(Mode mode, GameProfile id, GlobalPos globalPos) {
        this.mode = mode;
        this.playerUniqueId = id;
        this.playerPos = globalPos;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.mode);
        switch (this.mode) {
            case REMOVE_WAYPOINT -> buf.writeResourceLocation(this.waypointId);
            case ADD_WAYPOINT -> buf.writeResourceLocation(this.waypointId).writeUtf(GSON.toJson(this.waypoint));
            case REMOVE_TRIGGER -> buf.writeResourceLocation(this.triggerId);
            case ADD_TRIGGER -> buf.writeResourceLocation(this.triggerId).writeUtf(GSON.toJson(this.trigger));
            case UPDATE_POSITION -> {
                buf.writeGameProfile(this.playerUniqueId);
                buf.writeGlobalPos(this.playerPos);
            }
            case REMOVE_POSITION -> buf.writeGameProfile(this.playerUniqueId);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextGetter) {
        contextGetter.get().enqueueWork(() -> {
            switch (this.mode) {
                case ADD_WAYPOINT -> SignMeUpClient.MANAGER.addWaypoint(this.waypointId, this.waypoint);
                case REMOVE_WAYPOINT -> SignMeUpClient.MANAGER.removeWaypoint(this.waypointId);
                case ADD_TRIGGER -> SignMeUpClient.MANAGER.addTrigger(this.triggerId, this.trigger);
                case REMOVE_TRIGGER -> SignMeUpClient.MANAGER.removeTrigger(this.triggerId);
                case UPDATE_POSITION -> SignMeUpClient.MANAGER.updatePosition(this.playerUniqueId, this.playerPos);
                case REMOVE_POSITION -> SignMeUpClient.MANAGER.removePosition(this.playerUniqueId);
            }
            final Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof GuideMapScreen screen) {
                screen.refresh();
            }
        });
        contextGetter.get().setPacketHandled(true);
    }
}

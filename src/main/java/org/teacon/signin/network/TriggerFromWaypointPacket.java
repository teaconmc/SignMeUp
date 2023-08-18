package org.teacon.signin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.signin.SignMeUp;
import org.teacon.signin.data.entity.Waypoint;

import java.util.function.Supplier;

public final class TriggerFromWaypointPacket {

    private final ResourceLocation waypoint;
    private final ResourceLocation trigger;

    // Used for deserialization
    public TriggerFromWaypointPacket(FriendlyByteBuf buf) {
        this.waypoint = buf.readResourceLocation();
        this.trigger = buf.readResourceLocation();
    }

    public TriggerFromWaypointPacket(ResourceLocation waypoint, ResourceLocation trigger) {
        this.waypoint = waypoint;
        this.trigger = trigger;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.waypoint);
        buf.writeResourceLocation(this.trigger);
    }

    public void handle(Supplier<NetworkEvent.Context> contextGetter) {
        final NetworkEvent.Context context = contextGetter.get();
        context.enqueueWork(() -> {
            final Waypoint wp = SignMeUp.MANAGER.findWaypoint(this.waypoint);
            if (wp != null) {
                SignMeUp.trigger(context.getSender(), wp.getActualLocation(), this.trigger, false);
            }
        });
        context.setPacketHandled(true);
    }
}

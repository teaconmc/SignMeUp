package org.teacon.signin.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import org.teacon.signin.SignMeUp;
import org.teacon.signin.data.GuideMap;

import java.util.function.Supplier;

public final class TriggerFromMapPacket {

    private final ResourceLocation map;
    private final ResourceLocation trigger;

    // Used for deserialization
    public TriggerFromMapPacket(PacketBuffer buf) {
        this.map = buf.readResourceLocation();
        this.trigger = buf.readResourceLocation();
    }

    public TriggerFromMapPacket(ResourceLocation map, ResourceLocation trigger) {
        this.map = map;
        this.trigger = trigger;
    }

    public void write(PacketBuffer buf) {
        buf.writeResourceLocation(this.map);
        buf.writeResourceLocation(this.trigger);
    }

    public void handle(Supplier<NetworkEvent.Context> contextGetter) {
        final NetworkEvent.Context context = contextGetter.get();
        context.enqueueWork(() -> {
            final GuideMap map = SignMeUp.MANAGER.findMap(this.map);
            if (map != null) {
                SignMeUp.trigger(context.getSender(), map.center, this.trigger);
            }
        });
        context.setPacketHandled(true);
    }
}

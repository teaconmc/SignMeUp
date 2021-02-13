package org.teacon.signin.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import org.teacon.signin.SignMeUp;

import java.util.function.Supplier;

public class TriggerActivation {

    private ResourceLocation trigger;

    // Used for deserialization
    public TriggerActivation(PacketBuffer buf) {
        this.trigger = buf.readResourceLocation();
    }

    public TriggerActivation(ResourceLocation trigger) {
        this.trigger = trigger;
    }

    public void write(PacketBuffer buf) {
        buf.writeResourceLocation(this.trigger);
    }

    public void handle(Supplier<NetworkEvent.Context> contextGetter) {
        final NetworkEvent.Context context = contextGetter.get();
        context.enqueueWork(() -> SignMeUp.trigger(context.getSender(), this.trigger));
        context.setPacketHandled(true);
    }
}

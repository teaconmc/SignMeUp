package org.teacon.signin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.signin.client.SignMeUpClient;

import java.util.function.Supplier;

public final class MapScreenPacket {
    private final Action action;
    private final Vec3 position;
    private final ResourceLocation mapId;

    public MapScreenPacket(Action action, Vec3 position, ResourceLocation id) {
        this.position = position;
        this.action = action;
        this.mapId = id;
    }

    public MapScreenPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        if (this.action != Action.CLOSE_ANY) {
            this.position =  new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            this.mapId = buf.readResourceLocation();
        } else {
            this.position = Vec3.ZERO;
            this.mapId = null;
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
        if (this.action != Action.CLOSE_ANY) {
            buf.writeDouble(this.position.x());
            buf.writeDouble(this.position.y());
            buf.writeDouble(this.position.z());
            buf.writeResourceLocation(this.mapId);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextGetter) {
        SignMeUpClient.MANAGER.openMapByPacket(this.action, this.mapId, this.position);
        contextGetter.get().setPacketHandled(true);
    }

    public enum Action {
        OPEN_SPECIFIC, CLOSE_SPECIFIC, CLOSE_ANY
    }
}

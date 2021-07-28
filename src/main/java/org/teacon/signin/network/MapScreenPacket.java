package org.teacon.signin.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkEvent;
import org.teacon.signin.client.SignMeUpClient;

import java.util.function.Supplier;

public final class MapScreenPacket {
    private final Action action;
    private final Vector3d position;
    private final ResourceLocation mapId;

    public MapScreenPacket(Action action, Vector3d position, ResourceLocation id) {
        this.position = position;
        this.action = action;
        this.mapId = id;
    }

    public MapScreenPacket(PacketBuffer buf) {
        this.action = buf.readEnumValue(Action.class);
        if (this.action != Action.CLOSE_ANY) {
            this.position =  new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
            this.mapId = buf.readResourceLocation();
        } else {
            this.position = Vector3d.ZERO;
            this.mapId = null;
        }
    }

    public void write(PacketBuffer buf) {
        buf.writeEnumValue(this.action);
        if (this.action != Action.CLOSE_ANY) {
            buf.writeDouble(this.position.getX());
            buf.writeDouble(this.position.getY());
            buf.writeDouble(this.position.getZ());
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

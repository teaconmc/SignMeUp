package org.teacon.signin.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import org.teacon.signin.client.GuideMapScreen;
import org.teacon.signin.client.SignMeUpClient;
import org.teacon.signin.data.GuideMap;

import java.util.function.Supplier;

public class MapScreenPacket {
    private final Action action;
    private final ResourceLocation mapId;

    public MapScreenPacket(Action action, ResourceLocation id) {
        this.action = action;
        this.mapId = id;
    }

    public MapScreenPacket(PacketBuffer buf) {
        this.action = buf.readEnumValue(Action.class);
        this.mapId = this.action != Action.CLOSE_ANY ? buf.readResourceLocation() : null;
    }

    public void write(PacketBuffer buf) {
        buf.writeEnumValue(this.action);
        if (this.action != Action.CLOSE_ANY) {
            buf.writeResourceLocation(this.mapId);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextGetter) {
        contextGetter.get().enqueueWork(() -> {
            final Minecraft mc = Minecraft.getInstance();
            if (this.action == Action.OPEN_SPECIFIC) {
                GuideMap map = SignMeUpClient.MANAGER.findMap(this.mapId);
                mc.displayGuiScreen(new GuideMapScreen(this.mapId, map));
            } else if (mc.currentScreen instanceof GuideMapScreen) {
                if (this.action != Action.CLOSE_SPECIFIC || ((GuideMapScreen) mc.currentScreen).mapId.equals(this.mapId)) {
                    mc.displayGuiScreen(null);
                }
            }
        });
        contextGetter.get().setPacketHandled(true);
    }

    public enum Action {
        OPEN_SPECIFIC, CLOSE_SPECIFIC, CLOSE_ANY
    }
}

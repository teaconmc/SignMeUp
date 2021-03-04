package org.teacon.signin.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import org.teacon.signin.SignMeUp;
import org.teacon.signin.client.GuideMapScreen;
import org.teacon.signin.client.SignMeUpClient;
import org.teacon.signin.data.GuideMap;

import java.nio.charset.StandardCharsets;
import java.util.SortedMap;
import java.util.function.Supplier;

public class MapScreenPacket {
    private final ResourceLocation mapId;

    public MapScreenPacket(ResourceLocation id) {
        this.mapId = id;
    }

    public MapScreenPacket(PacketBuffer buf) {
        this.mapId = buf.readResourceLocation();
    }

    public void write(PacketBuffer buf) {
        buf.writeResourceLocation(this.mapId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextGetter) {
        contextGetter.get().enqueueWork(() -> {
            GuideMap map = SignMeUpClient.MANAGER.findMap(this.mapId);
            Minecraft.getInstance().displayGuiScreen(new GuideMapScreen(map));
        });
        contextGetter.get().setPacketHandled(true);
    }
}

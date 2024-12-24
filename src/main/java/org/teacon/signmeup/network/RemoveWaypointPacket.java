package org.teacon.signmeup.network;

import cn.ussshenzhou.t88.network.annotation.ClientHandler;
import cn.ussshenzhou.t88.network.annotation.Codec;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.event.server.ServerLifecycleEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.config.waypoints.Waypoint;
import org.teacon.signmeup.gui.map.MapScreen;

import java.util.UUID;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = SignMeUp.MODID)
public record RemoveWaypointPacket(UUID uuid) {

    @Codec
    public static final StreamCodec<ByteBuf, RemoveWaypointPacket> STREAM_CODEC = StreamCodec.composite(
            Waypoint.UUID_CODEC,
            RemoveWaypointPacket::uuid,
            RemoveWaypointPacket::new
    );

    @ClientHandler
    public void clientHandler(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ServerLifecycleHooks.getCurrentServer() == null) {
                Waypoint.INSTANCES.remove(uuid);
            }

            MapScreen.refreshInstance();
        });
    }
}

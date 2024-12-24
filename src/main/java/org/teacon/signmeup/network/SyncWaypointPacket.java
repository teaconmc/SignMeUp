package org.teacon.signmeup.network;

import cn.ussshenzhou.t88.network.annotation.ClientHandler;
import cn.ussshenzhou.t88.network.annotation.Codec;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.config.waypoints.Waypoint;
import org.teacon.signmeup.gui.map.MapScreen;

import java.util.List;

@NetPacket(modid = SignMeUp.MODID)
public record SyncWaypointPacket(List<Waypoint> waypoints) {
    @Codec
    public static final StreamCodec<ByteBuf, SyncWaypointPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.<ByteBuf, Waypoint>list().apply(Waypoint.CODEC),
            SyncWaypointPacket::waypoints,
            SyncWaypointPacket::new
    );

    @ClientHandler
    public void clientHandler(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ServerLifecycleHooks.getCurrentServer() == null) {
                Waypoint.INSTANCES.clear();
                for (Waypoint waypoint : waypoints) {
                    Waypoint.INSTANCES.put(waypoint.uuid(), waypoint);
                }
            }

            MapScreen.refreshInstance();
        });
    }
}

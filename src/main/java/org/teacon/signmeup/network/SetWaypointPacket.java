package org.teacon.signmeup.network;

import cn.ussshenzhou.t88.network.annotation.ClientHandler;
import cn.ussshenzhou.t88.network.annotation.Codec;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.config.waypoints.Waypoint;
import org.teacon.signmeup.gui.map.MapScreen;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = SignMeUp.MODID)
public record SetWaypointPacket(Waypoint waypoint) {

    @Codec
    public static final StreamCodec<ByteBuf, SetWaypointPacket> STREAM_CODEC = StreamCodec.composite(
            Waypoint.CODEC,
            SetWaypointPacket::waypoint,
            SetWaypointPacket::new
    );

    @ClientHandler
    public void clientHandler(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ServerLifecycleHooks.getCurrentServer() == null) {
                Waypoint.INSTANCES.put(waypoint.name, waypoint);
            }

            MapScreen.refreshInstance();
        });
    }
}

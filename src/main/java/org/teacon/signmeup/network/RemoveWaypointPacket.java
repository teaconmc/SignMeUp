package org.teacon.signmeup.network;

import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.network.annotation.ClientHandler;
import cn.ussshenzhou.t88.network.annotation.Codec;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.config.Waypoints;
import org.teacon.signmeup.gui.map.MapScreen;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = SignMeUp.MODID)
public record RemoveWaypointPacket(String name) {

    @Codec
    public static final StreamCodec<ByteBuf, RemoveWaypointPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            RemoveWaypointPacket::name,
            RemoveWaypointPacket::new
    );

    @ClientHandler
    public void clientHandler(IPayloadContext context) {
        ConfigHelper.getConfigWrite(Waypoints.class, waypoints -> waypoints.waypoints.remove(Waypoints.WayPoint.dumbWayPoint(name)));
        context.enqueueWork(MapScreen::refreshInstance);
    }
}

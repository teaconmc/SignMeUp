package org.teacon.signmeup.network;

import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.network.annotation.ClientHandler;
import cn.ussshenzhou.t88.network.annotation.Codec;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
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
public record SetWaypointPacket(String name, String description, BlockPos pos, Rotations rotation) {

    @Codec
    public static final StreamCodec<ByteBuf, SetWaypointPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SetWaypointPacket::name,
            ByteBufCodecs.STRING_UTF8,
            SetWaypointPacket::description,
            BlockPos.STREAM_CODEC,
            SetWaypointPacket::pos,
            Rotations.STREAM_CODEC,
            SetWaypointPacket::rotation,
            SetWaypointPacket::new
    );

    @ClientHandler
    public void clientHandler(IPayloadContext context) {
        var waypoint = new Waypoints.WayPoint(name, description, pos.getX(), pos.getY(), pos.getZ(), rotation.getX(), rotation.getZ());
        context.enqueueWork(() -> {
            ConfigHelper.getConfigWrite(Waypoints.class, waypoints -> waypoints.waypoints.add(waypoint));

            MapScreen.refreshInstance();
        });
    }
}

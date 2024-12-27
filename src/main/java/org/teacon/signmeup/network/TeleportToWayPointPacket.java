package org.teacon.signmeup.network;

import cn.ussshenzhou.t88.network.annotation.Codec;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import cn.ussshenzhou.t88.network.annotation.ServerHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.config.waypoints.Waypoint;

import java.util.Set;
import java.util.UUID;

/**
 * @author USS_Shenzhou
 */
@NetPacket(modid = SignMeUp.MODID)
public record TeleportToWayPointPacket(UUID id) {
    @Codec
    public static final StreamCodec<ByteBuf, TeleportToWayPointPacket> STREAM_CODEC = StreamCodec.composite(
            Waypoint.UUID_CODEC,
            TeleportToWayPointPacket::id,
            TeleportToWayPointPacket::new
    );

    @ServerHandler
    public void serverHandler(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (!(player.level() instanceof ServerLevel level) || level.dimension() != Level .OVERWORLD) {
                return;
            }

            Waypoint waypoint = Waypoint.INSTANCES.get(id);
            if (waypoint != null) {
                player.teleportTo(level, waypoint.pos().x(), waypoint.pos().y(), waypoint.pos().z(), Set.of(), waypoint.rotation().x(), waypoint.rotation().y());
            } else {
                player.sendSystemMessage(Component.literal("Open a new SMU map! The waypoint data is out of date."));
            }
        });
    }
}

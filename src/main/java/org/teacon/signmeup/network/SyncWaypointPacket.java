package org.teacon.signmeup.network;

import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.network.annotation.ClientHandler;
import cn.ussshenzhou.t88.network.annotation.Codec;
import cn.ussshenzhou.t88.network.annotation.NetPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.config.Waypoints;
import org.teacon.signmeup.gui.map.MapScreen;

import java.util.List;

@NetPacket(modid = SignMeUp.MODID)
public record SyncWaypointPacket(List<Waypoints.WayPoint> waypoints) {
    @Codec
    public static final StreamCodec<ByteBuf, SyncWaypointPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.<ByteBuf, Waypoints.WayPoint>list().apply(new StreamCodec<>() {
                @Override
                public Waypoints.@NotNull WayPoint decode(@NotNull ByteBuf buffer) {
                    return new Waypoints.WayPoint(
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.FLOAT.decode(buffer),
                            ByteBufCodecs.FLOAT.decode(buffer)
                    );
                }

                @Override
                public void encode(@NotNull ByteBuf buffer, Waypoints.@NotNull WayPoint value) {
                    ByteBufCodecs.STRING_UTF8.encode(buffer, value.name);
                    ByteBufCodecs.STRING_UTF8.encode(buffer, value.description);
                    ByteBufCodecs.VAR_INT.encode(buffer, value.x);
                    ByteBufCodecs.VAR_INT.encode(buffer, value.y);
                    ByteBufCodecs.VAR_INT.encode(buffer, value.z);
                    ByteBufCodecs.FLOAT.encode(buffer, value.rx);
                    ByteBufCodecs.FLOAT.encode(buffer, value.ry);
                }
            }),
            SyncWaypointPacket::waypoints,
            SyncWaypointPacket::new
    );

    @ClientHandler
    public void clientHandler(IPayloadContext context) {
        context.enqueueWork(() -> {
            ConfigHelper.getConfigWrite(Waypoints.class, waypoints -> {
                waypoints.waypoints.clear();
                waypoints.waypoints.addAll(this.waypoints);
            });

            MapScreen.refreshInstance();
        });
    }
}

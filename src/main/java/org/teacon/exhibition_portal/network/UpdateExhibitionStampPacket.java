package org.teacon.exhibition_portal.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.jetbrains.annotations.NotNull;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.components.EPServer;
import org.teacon.exhibition_portal.components.ExhibitionDeclaration;
import org.teacon.exhibition_portal.components.ExhibitionStamp;

import java.util.UUID;

@EventBusSubscriber(modid = ExhibitionPortal.MODID)
public record UpdateExhibitionStampPacket(UUID exhibition, ExhibitionStamp stamp) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, UpdateExhibitionStampPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, UpdateExhibitionStampPacket::exhibition,
            ExhibitionStamp.STREAM_CODEC, UpdateExhibitionStampPacket::stamp,
            UpdateExhibitionStampPacket::new
    );

    public static Type<UpdateExhibitionStampPacket> TYPE = new Type<>(ExhibitionPortal.id("s2c/update_exhibition_stamp"));

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @SubscribeEvent
    private static void on(RegisterPayloadHandlersEvent event) {
        event.registrar(ExhibitionPortal.VERSION).playToServer(TYPE, STREAM_CODEC, (packet, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ExhibitionDeclaration declaration = EPServer.getDeclarationOrNull(packet.exhibition);
            if (declaration != null) {
                EPServer.updateFootprint(player, packet.exhibition, f -> {
                    for (ExhibitionStamp stamp : f.stamps()) {
                        if (stamp.id().equalsIgnoreCase(packet.stamp.id())) {
                            return f.withStamp(packet.stamp);
                        }
                    }
                    return f;
                });
            }
        });
    }
}

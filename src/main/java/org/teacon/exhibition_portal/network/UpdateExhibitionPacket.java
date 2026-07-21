package org.teacon.exhibition_portal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.jetbrains.annotations.NotNull;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.components.Exhibition;

import java.util.List;

@EventBusSubscriber(modid = ExhibitionPortal.MODID)
public record UpdateExhibitionPacket(List<Exhibition> galleries) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, UpdateExhibitionPacket> STREAM_CODEC = StreamCodec.composite(
            Exhibition.STREAM_CODEC.apply(ByteBufCodecs.list()), UpdateExhibitionPacket::galleries,
            UpdateExhibitionPacket::new
    );

    public static Type<UpdateExhibitionPacket> TYPE = new Type<>(ExhibitionPortal.id("s2c/sync_exhibitions"));

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @SubscribeEvent
    private static void on(RegisterPayloadHandlersEvent event) {
        event.registrar(ExhibitionPortal.VERSION).playToClient(TYPE, STREAM_CODEC);
    }
}

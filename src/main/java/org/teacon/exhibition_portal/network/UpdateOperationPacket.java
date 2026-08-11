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
import org.teacon.exhibition_portal.components.EPOperation;

@EventBusSubscriber(modid = ExhibitionPortal.MODID)
public record UpdateOperationPacket(EPOperation operation) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, UpdateOperationPacket> STREAM_CODEC = StreamCodec.composite(
            StreamCodec.composite(
                    StreamCodec.composite(
                            ByteBufCodecs.STRING_UTF8, EPOperation.Item::id,
                            ByteBufCodecs.STRING_UTF8, EPOperation.Item::title,
                            ByteBufCodecs.STRING_UTF8, EPOperation.Item::tooltip,
                            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), EPOperation.Item::commands,
                            EPOperation.Item::new
                    ).apply(ByteBufCodecs.list()), EPOperation::operations,
                    EPOperation::new
            ), UpdateOperationPacket::operation,
            UpdateOperationPacket::new
    );

    public static Type<UpdateOperationPacket> TYPE = new Type<>(ExhibitionPortal.id("s2c/sync_operations"));

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

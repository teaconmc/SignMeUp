package org.teacon.exhibition_portal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.components.EPServer;
import org.teacon.exhibition_portal.components.ExhibitionMetadata;

import java.util.List;

@EventBusSubscriber(modid = ExhibitionPortal.MODID)
public record UpdateExhibitionMetadataPacket(List<ExhibitionMetadata> metadata) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, UpdateExhibitionMetadataPacket> STREAM_CODEC = StreamCodec.composite(
            ExhibitionMetadata.STREAM_CODEC.apply(ByteBufCodecs.list()), UpdateExhibitionMetadataPacket::metadata,
            UpdateExhibitionMetadataPacket::new
    );

    public static Type<UpdateExhibitionMetadataPacket> TYPE = new Type<>(ExhibitionPortal.id("s2c/update_exhibition_metadata_packet"));

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @SubscribeEvent
    private static void on(RegisterPayloadHandlersEvent event) {
        event.registrar(ExhibitionPortal.VERSION).playToServer(TYPE, STREAM_CODEC, (packet, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null || (!server.isSingleplayer() && !player.permissions().hasPermission(Permissions.COMMANDS_OWNER))) {
                context.disconnect(Component.literal("Illegal access."));
                return;
            }

            server.execute(() -> EPServer.replaceMetadata(packet.metadata));
        });
    }
}

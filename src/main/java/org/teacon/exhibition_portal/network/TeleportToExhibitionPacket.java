package org.teacon.exhibition_portal.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.components.EPServer;
import org.teacon.exhibition_portal.components.ExhibitionDeclaration;
import org.teacon.exhibition_portal.components.ExhibitionFootprint;
import org.teacon.exhibition_portal.components.ExhibitionMetadata;
import org.teacon.exhibition_portal.components.ExhibitionWaypoint;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = ExhibitionPortal.MODID)
public record TeleportToExhibitionPacket(UUID exhibition) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, TeleportToExhibitionPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, TeleportToExhibitionPacket::exhibition,
            TeleportToExhibitionPacket::new
    );

    public static Type<TeleportToExhibitionPacket> TYPE = new Type<>(ExhibitionPortal.id("s2c/teleport_to_exhibition"));

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @SubscribeEvent
    private static void on(RegisterPayloadHandlersEvent event) {
        event.registrar(ExhibitionPortal.VERSION).playToServer(TYPE, STREAM_CODEC, (packet, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();

            ExhibitionDeclaration declaration = EPServer.getDeclarationOrNull(packet.exhibition());
            if (declaration == null) {
                return;
            }

            ExhibitionMetadata metadata = EPServer.getMetadata(packet.exhibition());
            ExhibitionWaypoint waypoint = metadata.waypoint();
            player.teleportTo(
                    Objects.requireNonNull(Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer()).getLevel(Level.OVERWORLD)),
                    waypoint.x() + 0.5, waypoint.y() + 0.5, waypoint.z() + 0.5,
                    Set.of(), waypoint.ry(), waypoint.rx(), true // DO NOT EDIT. It's intended to be (ry, rx)
            );
            player.sendOverlayMessage(Component.literal(metadata.name()).withoutShadow());
            EPServer.updateFootprint(player, packet.exhibition(), f -> f.withMark(ExhibitionFootprint.MARK_VISITED));
        });
    }
}

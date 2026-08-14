package org.teacon.exhibition_portal.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.teacon.exhibition_portal.ExhibitionPortal;

@EventBusSubscriber(modid = ExhibitionPortal.MODID)
public record TeleportToPositionPacket(int x, int z) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, TeleportToPositionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TeleportToPositionPacket::x,
            ByteBufCodecs.VAR_INT, TeleportToPositionPacket::z,
            TeleportToPositionPacket::new
    );

    public static Type<TeleportToPositionPacket> TYPE = new Type<>(ExhibitionPortal.id("s2c/teleport_to_position"));

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
            if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || server == null) {
                return;
            }

            server.execute(() -> {
                int x = packet.x, z = packet.z;
                ChunkAccess chunk = server.overworld().getChunk(new BlockPos(x, 0, z));
                int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                player.teleportTo(x + 0.5f, height + 1.5f, z + 0.5f);
            });
        });
    }
}

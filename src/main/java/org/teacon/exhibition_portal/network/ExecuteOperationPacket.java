package org.teacon.exhibition_portal.network;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.components.EPOperation;
import org.teacon.exhibition_portal.components.EPServer;

import java.util.Objects;

@EventBusSubscriber(modid = ExhibitionPortal.MODID)
public record ExecuteOperationPacket(String operationID) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, ExecuteOperationPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ExecuteOperationPacket::operationID,
            ExecuteOperationPacket::new
    );

    public static Type<ExecuteOperationPacket> TYPE = new Type<>(ExhibitionPortal.id("s2c/execute_operation"));

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @SubscribeEvent
    private static void on(RegisterPayloadHandlersEvent event) {
        event.registrar(ExhibitionPortal.VERSION).playToServer(TYPE, STREAM_CODEC, (packet, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            EPOperation operation = EPServer.OPERATIONS;
            if (operation != null) {
                for (EPOperation.Item item : operation.operations()) {
                    if (item.id().equals(packet.operationID)) {
                        MinecraftServer server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer());
                        CommandSourceStack source = server
                                .createCommandSourceStack()
                                .withEntity(player)
                                .withSuppressedOutput()
                                .withPermission(LevelBasedPermissionSet.GAMEMASTER);
                        for (String command : item.commands()) {
                            server.getCommands().performPrefixedCommand(source, command);
                        }
                        break;
                    }
                }
            }
        });
    }
}

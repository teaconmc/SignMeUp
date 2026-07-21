package org.teacon.exhibition_portal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.exhibition_portal.client.EPClient;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;
import org.teacon.exhibition_portal.components.Exhibition;
import org.teacon.exhibition_portal.components.ExhibitionMetadata;
import org.teacon.exhibition_portal.network.UpdateExhibitionMetadataPacket;
import org.teacon.exhibition_portal.utils.Components;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@EventBusSubscriber(Dist.CLIENT)
public final class EPClientCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(EPClientCommands.class);

    private EPClientCommands() {
    }

    @SubscribeEvent
    private static void on(RegisterClientCommandsEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || (minecraft.level == null && !minecraft.player.permissions().hasPermission(Permissions.COMMANDS_OWNER))) {
            return;
        }

        Codec<List<ExhibitionMetadata>> codec = ExhibitionMetadata.CODEC.codec().listOf();
        event.getDispatcher().register(Commands.literal("teacon").then(Commands.literal("exhibitionc")
                .then(
                        Commands.literal("get_exhibition_metadata").executes(context -> {
                            List<ExhibitionMetadata> metadata = RenderAccess.get(EPClient.GALLERY_LOOKUP).values().stream()
                                    .map(Exhibition::metadata)
                                    .toList();

                            try {
                                Path file = Files.createTempFile("exhibition-metadata-", ".dat");
                                CompoundTag ctag = new CompoundTag();
                                ctag.put("", codec.encode(metadata, NbtOps.INSTANCE, new ListTag()).getOrThrow());
                                NbtIo.writeCompressed(ctag, file);
                                context.getSource().sendSystemMessage(
                                        Components.join(
                                                Component.literal("Successfully execute get_exhibition_metadata: "),
                                                Component.literal(file.toAbsolutePath().toString()).withStyle(
                                                        s -> s.withClickEvent(new ClickEvent.OpenFile(file))
                                                )
                                        )
                                );
                            } catch (Throwable t) {
                                context.getSource().sendSystemMessage(Component.literal("Cannot execute get_exhibition_metadata"));
                                LOGGER.warn("Cannot execute get_exhibition_metadata", t);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(Commands.literal("update_exhibition_metadata").then(
                        Commands.argument("path", StringArgumentType.greedyString()).executes(context -> {
                            String path = context.getArgument("path", String.class);
                            try {
                                Path file = Path.of(path);
                                CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                                List<ExhibitionMetadata> metadata = codec.decode(NbtOps.INSTANCE, tag.getList("").orElseThrow()).getOrThrow().getFirst();
                                Objects.requireNonNull(Minecraft.getInstance().getConnection()).send(new UpdateExhibitionMetadataPacket(metadata));
                                context.getSource().sendSystemMessage(Component.literal("Successfully execute update_exhibition_metadata."));
                            } catch (Throwable t) {
                                LOGGER.warn("Cannot execute update_exhibition_metadata: {}", path, t);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                ))
        ));
    }
}

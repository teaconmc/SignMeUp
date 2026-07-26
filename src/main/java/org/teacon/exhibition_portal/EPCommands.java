package org.teacon.exhibition_portal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec2;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.teacon.exhibition_portal.client.interaction.StampingCounterBlockEntity;
import org.teacon.exhibition_portal.client.screens.map.MapScreenLayouts;
import org.teacon.exhibition_portal.components.EPServer;
import org.teacon.exhibition_portal.components.ExhibitionDeclaration;
import org.teacon.exhibition_portal.components.ExhibitionMetadata;
import org.teacon.exhibition_portal.components.ExhibitionWaypoint;
import org.teacon.exhibition_portal.utils.Components;
import org.teacon.exhibition_portal.utils.EnumStringArgument;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = ExhibitionPortal.MODID)
public final class EPCommands {
    private EPCommands() {
    }

    private static final class PlayerAccess {
        private static final Class<?> CLAZZ;
        private static final VarHandle SERVER_PLAYER;

        static {
            List<Class<?>> candidates = new ArrayList<>(5);
            for (int i = 1; true; i++) {
                try {
                    candidates.add(Class.forName(ServerPlayer.class.getName() + "$" + i));
                } catch (ClassNotFoundException e) {
                    break;
                }
            }

            List<Field> list = candidates.stream()
                    .filter(CommandSource.class::isAssignableFrom)
                    .flatMap(clazz -> Arrays.stream(clazz.getDeclaredFields()))
                    .filter(field -> field.getType() == ServerPlayer.class)
                    .toList();
            if (list.size() != 1) {
                throw new AssertionError("Cannot locate ServerPlayer$3 from candidates: " + list);
            }

            Field field = list.getFirst();

            try {
                CLAZZ = field.getDeclaringClass();
                SERVER_PLAYER = MethodHandles.privateLookupIn(ServerPlayer.class, MethodHandles.lookup())
                        .findVarHandle(CLAZZ, field.getName(), ServerPlayer.class)
                        .withInvokeBehavior();
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        public static boolean is(CommandSourceStack stack) {
            return CLAZZ.isInstance(stack.source);
        }

        public static ServerPlayer get(CommandContext<CommandSourceStack> context) {
            CommandSource source = context.getSource().source;
            return (ServerPlayer) SERVER_PLAYER.get(source);
        }
    }

    @SubscribeEvent
    private static void on(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("teacon").then(Commands.literal("exhibition").requires(PlayerAccess::is).then(
                Commands.literal("mine").executes(context -> {
                    ServerPlayer player = PlayerAccess.get(context);

                    MutableComponent message = Component.translatable("exhibition_portal.list.header");
                    List<UUID> galleries = EPServer.getOwned(player);
                    galleries.sort(null);
                    for (UUID uuid : galleries) {
                        message = message.append("\n").append(ofExhibitionSummary(EPServer.getDeclaration(uuid), EPServer.getMetadata(uuid)));
                    }

                    context.getSource().sendSystemMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
        ).then(
                Commands.literal("debug").requires(PlayerAccess::is).then(
                        Commands.literal("clear_footprint").executes(context -> {
                            ServerPlayer player = PlayerAccess.get(context);
                            EPServer.clearFootprint(player);
                            return Command.SINGLE_SUCCESS;
                        })
                )
        ).then(
                Commands.argument("uuid", UuidArgument.uuid())
                        .executes(context -> {
                            UUID uuid = UuidArgument.getUuid(context, "uuid");
                            ExhibitionDeclaration exhibition = EPServer.getDeclarationOrNull(uuid);
                            if (exhibition == null) {
                                context.getSource().sendSystemMessage(Component.literal("No such exhibition: " + uuid));
                                return -1;
                            }

                            sendExhibitionDetail(context, exhibition, EPServer.getMetadata(exhibition.uuid()));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.literal("stamp").then(
                                Commands.argument("stamp_id", new EnumStringArgument(EPServer.ALLOWED_STAMP_IDS)).then(
                                        Commands.argument("item", IdentifierArgument.id())
                                                .executes(context -> {
                                                    UUID uuid = UuidArgument.getUuid(context, "uuid");
                                                    String stampID = context.getArgument("stamp_id", String.class);
                                                    Identifier item = IdentifierArgument.getId(context, "item");

                                                    ServerPlayer player = PlayerAccess.get(context);

                                                    ExhibitionDeclaration exhibition = EPServer.getDeclarationOrNull(uuid);
                                                    if (exhibition == null || nonAccess(exhibition, player)) {
                                                        context.getSource().sendFailure(Component.literal("Permission denied."));
                                                        return -1;
                                                    }

                                                    ItemStack stack = new ItemStack(ExhibitionPortal.STAMPING_COUNTER_ITEM.get());

                                                    TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,context.getSource().registryAccess());
                                                    StampingCounterBlockEntity.saveStamp(output, uuid, stampID, item);
                                                    BlockItem.setBlockEntityData(stack, ExhibitionPortal.STAMPING_COUNTER_BE.get(), output);

                                                    player.getInventory().placeItemBackInInventory(stack);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                )
                        ))
                        .then(
                                Commands.literal("set").then(
                                        Commands.literal("name").then(
                                                Commands.argument("name", StringArgumentType.greedyString())
                                                        .executes(context -> {
                                                            UUID uuid = UuidArgument.getUuid(context, "uuid");
                                                            String name = context.getArgument("name", String.class);

                                                            ExhibitionDeclaration exhibition = EPServer.getDeclarationOrNull(uuid);
                                                            if (exhibition == null || nonAccess(exhibition, PlayerAccess.get(context))) {
                                                                context.getSource().sendFailure(Component.literal("Permission denied."));
                                                                return -1;
                                                            }

                                                            ExhibitionMetadata metadata = EPServer.updateMetadata(uuid, previous -> previous.withName(name));
                                                            sendExhibitionDetail(context, EPServer.getDeclaration(uuid), metadata);

                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                        )
                                ).then(
                                        Commands.literal("description").then(
                                                Commands.argument("description", StringArgumentType.greedyString())
                                                        .executes(context -> {
                                                            UUID uuid = UuidArgument.getUuid(context, "uuid");
                                                            String description = context.getArgument("description", String.class);

                                                            ExhibitionDeclaration exhibition = EPServer.getDeclarationOrNull(uuid);
                                                            if (exhibition == null || nonAccess(exhibition, PlayerAccess.get(context))) {
                                                                context.getSource().sendFailure(Component.literal("Permission denied."));
                                                                return -1;
                                                            }

                                                            ExhibitionMetadata metadata = EPServer.updateMetadata(uuid, legacy -> legacy.withDescription(description));
                                                            sendExhibitionDetail(context, EPServer.getDeclaration(uuid), metadata);
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                        )
                                ).then(
                                        Commands.literal("introduction").then(
                                                Commands.argument("introduction", StringArgumentType.greedyString())
                                                        .executes(context -> {
                                                            UUID uuid = UuidArgument.getUuid(context, "uuid");
                                                            String introduction = context.getArgument("introduction", String.class);

                                                            ExhibitionDeclaration exhibition = EPServer.getDeclarationOrNull(uuid);
                                                            if (exhibition == null || nonAccess(exhibition, PlayerAccess.get(context))) {
                                                                context.getSource().sendFailure(Component.literal("Permission denied."));
                                                                return -1;
                                                            }

                                                            ExhibitionMetadata metadata = EPServer.updateMetadata(uuid, legacy -> legacy.withIntroduction(introduction));
                                                            sendExhibitionDetail(context, EPServer.getDeclaration(uuid), metadata);
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                        )
                                ).then(
                                        Commands.literal("evt").then(
                                                Commands.argument("evtMin", IntegerArgumentType.integer(0, 127)).then(
                                                        Commands.argument("evtMax", IntegerArgumentType.integer(-1, 127))
                                                                .executes(context -> {
                                                                    UUID uuid = UuidArgument.getUuid(context, "uuid");
                                                                    ExhibitionDeclaration exhibition = EPServer.getDeclarationOrNull(uuid);
                                                                    if (exhibition == null || nonAccess(exhibition, PlayerAccess.get(context))) {
                                                                        context.getSource().sendFailure(Component.literal("Permission denied."));
                                                                        return -1;
                                                                    }

                                                                    int evtMax = context.getArgument("evtMax", int.class);
                                                                    int evtMin = context.getArgument("evtMin", int.class);

                                                                    ExhibitionMetadata metadata = EPServer.updateMetadata(uuid, legacy -> legacy.withEVT((byte) evtMin, (byte) evtMax));
                                                                    sendExhibitionDetail(context, EPServer.getDeclaration(uuid), metadata);
                                                                    return Command.SINGLE_SUCCESS;
                                                                })
                                                )
                                        )
                                ).then(
                                        Commands.literal("waypoint")
                                                .executes(context -> {
                                                    UUID uuid = UuidArgument.getUuid(context, "uuid");
                                                    ServerPlayer player = PlayerAccess.get(context);

                                                    ExhibitionDeclaration exhibition = EPServer.getDeclarationOrNull(uuid);
                                                    if (exhibition == null || nonAccess(exhibition, player)) {
                                                        context.getSource().sendFailure(Component.literal("Permission denied."));
                                                        return -1;
                                                    }

                                                    BlockPos pos = player.blockPosition();
                                                    Vec2 rotation = player.getRotationVector();
                                                    ExhibitionWaypoint waypoint = new ExhibitionWaypoint(pos.getX(), pos.getY(), pos.getZ(), rotation.x, rotation.y);

                                                    ExhibitionMetadata metadata = EPServer.updateMetadata(uuid, (legacy) -> legacy.withWaypoint(waypoint));
                                                    sendExhibitionDetail(context, EPServer.getDeclaration(uuid), metadata);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                )
                        )
        )));
    }

    private static boolean nonAccess(ExhibitionDeclaration exhibition, ServerPlayer player) {
        if (!FMLEnvironment.isProduction()) {
            return false;
        }
        if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return true;
        }
        if (player.permissions().hasPermission(Permissions.COMMANDS_OWNER)) {
            return false;
        }
        return !exhibition.administrators().contains(player.getUUID());
    }

    private static void sendExhibitionDetail(CommandContext<CommandSourceStack> context, ExhibitionDeclaration exhibition, ExhibitionMetadata metadata) {
        context.getSource().sendSystemMessage(Components.join(
                Component.translatable("exhibition_portal.detail.header", metadata.uuid().toString()),
                Component.literal("\n"),
                ofExhibitionKV(exhibition, "name", metadata.name(), " "),
                Component.literal("\n"),
                ofExhibitionKV(exhibition, "description", metadata.description(), " "),
                Component.literal("\n"),
                ofExhibitionKV(exhibition, "introduction", metadata.introduction(), " "),
                Component.literal("\n"),
                ofExhibitionKV(exhibition, "evt", MapScreenLayouts.ofEVT(metadata), " "),
                Component.literal("\n"),
                ofExhibitionKV(exhibition, "waypoint", metadata.waypoint().toUserString(), ""),
                Component.literal("\n"),
                Component.translatable("exhibition_portal.request_stamp_table").withColor(0x2596be)
                        .withStyle(s -> s.withClickEvent(new ClickEvent.SuggestCommand("/teacon exhibition " + exhibition.uuid() + " stamp ")))
                        .withStyle(s -> s.withUnderlined(true))
        ));
    }

    private static MutableComponent ofExhibitionSummary(ExhibitionDeclaration exhibition, ExhibitionMetadata metadata) {
        return Components.join(
                Component.translatable("exhibition_portal.list.view")
                        .withColor(0x2596be)
                        .withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand("/teacon exhibition " + exhibition.uuid())))
                        .withStyle(s -> s.withUnderlined(true)),
                Component.literal(" "),
                Component.literal("[" + String.join(",", exhibition.mods()) + "]")
                        .withColor(0x9925be)
                        .withStyle(s -> s.withItalic(true)),
                Component.literal(" " + metadata.name() + ": " + metadata.description())
        );
    }

    private static MutableComponent ofExhibitionKV(ExhibitionDeclaration exhibition, String key, String value, String tail) {
        return ofExhibitionKV(exhibition, key, Component.literal(value), tail);
    }

    private static MutableComponent ofExhibitionKV(ExhibitionDeclaration exhibition, String key, Component value, String tail) {
        return Components.join(
                Component.translatable("exhibition_portal.detail.tweak")
                        .withColor(0x2596be)
                        .withStyle(s -> s.withClickEvent(new ClickEvent.SuggestCommand("/teacon exhibition " + exhibition.uuid() + " set " + key + tail)))
                        .withStyle(s -> s.withUnderlined(true)),
                Component.literal(" "),
                Component.translatable("exhibition_portal.detail.key." + key)
                        .withColor(0x9925be)
                        .withStyle(s -> s.withItalic(true)),
                Component.literal(": "),
                value
        );
    }
}

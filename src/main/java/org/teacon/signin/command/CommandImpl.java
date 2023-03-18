package org.teacon.signin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Function4;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import org.teacon.signin.SignMeUp;
import org.teacon.signin.data.DynamicLocationStorage;
import org.teacon.signin.data.GuideMap;
import org.teacon.signin.data.Waypoint;
import org.teacon.signin.network.MapScreenPacket;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * This class contains command execution implementations
 */
public final class CommandImpl {

    public static int trigger(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ResourceLocation triggerId = ResourceLocationArgument.getId(context, "id");
        final BlockPos pos = BlockPos.containing(context.getSource().getPosition());
        final ServerPlayer src = context.getSource().getPlayerOrException();
        return SignMeUp.trigger(src, pos, triggerId, true) ? 1 : 0;
    }

    public static int listMaps(CommandContext<CommandSourceStack> context) {
        CommandSourceStack src = context.getSource();
        if (SignMeUp.MANAGER.getAllMaps().size() != 0) {
            src.sendSuccess(Component.translatable("sign_up.text.list_maps").append(": "), false);
            for (GuideMap map : SignMeUp.MANAGER.getAllMaps()) {
                src.sendSuccess(map.getTitle(), false);
            }
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(Component.translatable("sign_up.text.error")
                    .append(": ")
                    .append(Component.translatable("sign_up.text.no_map_exists")));
            return -1;
        }
    }

    public static int closeSpecificMap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack src = context.getSource();
        ServerPlayer player = context.getSource().getPlayerOrException();
        final ResourceLocation id = context.getArgument("id", ResourceLocation.class);
        GuideMap map = SignMeUp.MANAGER.findMap(id);
        if (map != null) {
            // Here we have to send a packet to client side
            // for rendering the map GUI
            MapScreenPacket packet = new MapScreenPacket(MapScreenPacket.Action.CLOSE_SPECIFIC, src.getPosition(), id);
            SignMeUp.channel.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(Component.translatable("sign_up.text.error")
                    .append(": ")
                    .append(Component.translatable("sign_up.text.map"))
                    .append(" " + id.toString() + " ")
                    .append(Component.translatable("sign_up.text.does_not_exist")));
            return -1;
        }
    }

    public static int closeAnyMap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MapScreenPacket packet = new MapScreenPacket(MapScreenPacket.Action.CLOSE_ANY, Vec3.ZERO, null);
        SignMeUp.channel.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        return Command.SINGLE_SUCCESS;
    }

    public static int openSpecificMap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack src = context.getSource();
        ServerPlayer player = context.getSource().getPlayerOrException();
        final ResourceLocation id = context.getArgument("id", ResourceLocation.class);
        GuideMap map = SignMeUp.MANAGER.findMap(id);
        if (map != null) {
            // Here we have to send a packet to client side
            // for rendering the map GUI
            final MapScreenPacket packet = new MapScreenPacket(MapScreenPacket.Action.OPEN_SPECIFIC, src.getPosition(), id);
            SignMeUp.channel.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(Component.translatable("sign_up.text.error")
                    .append(": ")
                    .append(Component.translatable("sign_up.text.map"))
                    .append(" " + id.toString() + " ")
                    .append(Component.translatable("sign_up.text.does_not_exist")));
            return -1;
        }
    }

    public static int openNearestMap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack src = context.getSource();
        ServerPlayer player = src.getPlayerOrException();
        ResourceKey<Level> worldKey = src.getLevel().dimension();

        GuideMap map = null;
        double minDistanceSq = Double.MAX_VALUE;

        // We first check the dimension
        if (src.getPlayerOrException().getLevel().dimension() == worldKey) {
            // Then we look for the nearest in-range map
            for (GuideMap guideMap : SignMeUp.MANAGER.getAllMaps()) {
                final double dx = src.getPosition().x() - guideMap.center.getX();
                final double dz = src.getPosition().z() - guideMap.center.getZ();
                if (Math.min(Math.abs(dx), Math.abs(dz)) <= guideMap.radius) {
                    final double distanceSq = dx * dx + dz * dz;
                    if (distanceSq < minDistanceSq) {
                        minDistanceSq = distanceSq;
                        map = guideMap;
                    }
                }
            }
        }

        if (map != null) {
            // Same packet as above
            final MapScreenPacket packet = new MapScreenPacket(MapScreenPacket.Action.OPEN_SPECIFIC, src.getPosition(), SignMeUp.MANAGER.findMapId(map));
            SignMeUp.channel.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(Component.translatable("sign_up.text.error")
                    .append(": ")
                    .append(Component.translatable("sign_up.status.no_map_available")));
            return -1;
        }
    }

    public static int listWaypoints(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack src = context.getSource();
        ServerPlayer player = src.getPlayerOrException();
        if (SignMeUp.MANAGER.getAllWaypoints().size() != 0) {
            src.sendSuccess(Component.translatable("sign_up.text.list_points")
                    .append(": ")
                    , false);
            for (Waypoint waypoint : SignMeUp.MANAGER.getAllWaypoints()) {
                DecimalFormat df = new DecimalFormat("0.00");
                df.setRoundingMode(RoundingMode.HALF_UP);
                src.sendSuccess(Component.literal(" - ")
                        .append(waypoint.getTitle()).append("\n   ")
                        .append(Component.translatable("sign_up.text.distance"))
                        .append(": " + df.format(Vec3.atLowerCornerOf(waypoint.getActualLocation()).distanceTo(src.getPosition())) + " ")
                        .append(Component.translatable("sign_up.text.blocks_away"))
                        , false
                );
            }
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(Component.translatable("sign_up.text.error").append(": ").append(Component.translatable("sign_up.text.no_waypoint_exists")));
            return -1;
        }
    }

    public static int listWaypointPos(CommandContext<CommandSourceStack> context) {
        CommandSourceStack src = context.getSource();
        if (SignMeUp.MANAGER.getAllWaypoints().size() != 0) {
            src.sendSuccess(Component.translatable("sign_up.text.list_points"), false);
            for (Waypoint waypoint : SignMeUp.MANAGER.getAllWaypoints()) {
                src.sendSuccess(Component.literal(" - ")
                        .append(waypoint.getTitle())
                        .append("\n   ")
                        .append(Component.translatable("sign_up.text.render_location"))
                        .append(": ")
                        .append(waypoint.getRenderLocation().toShortString())
                        .append("\n   ")
                        .append(Component.translatable("sign_up.text.actual_location"))
                        .append(": ")
                        .append(waypoint.getActualLocation().toShortString())
                        , false
                );
            }
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(Component.translatable("sign_up.text.error").append(": ").append(Component.translatable("sign_up.text.no_waypoint_exists")));
            return -1;
        }
    }

    public static int getWaypointPos(CommandContext<CommandSourceStack> context) {
        CommandSourceStack src = context.getSource();
        final ResourceLocation id = context.getArgument("id", ResourceLocation.class);
        Waypoint waypoint = SignMeUp.MANAGER.findWaypoint(id);
        if (waypoint != null) {
            src.sendSuccess(Component.literal(" - ")
                            .append(waypoint.getTitle())
                            .append("\n   ")
                            .append(Component.translatable("sign_up.text.render_location"))
                            .append(": ")
                            .append(waypoint.getRenderLocation().toShortString())
                            .append("\n   ")
                            .append(Component.translatable("sign_up.text.actual_location"))
                            .append(": ")
                            .append(waypoint.getActualLocation().toShortString())
                    , false
            );
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendFailure(Component.literal("Error: waypoint " + id + " does not exist"));
            src.sendFailure(Component.translatable("sign_up.text.error")
                    .append(": ")
                    .append(Component.translatable("sign_up.text.waypoint")
                    .append(" " + id.toString() + " ")
                    .append(Component.translatable("sign_up.text.does_not_exist")))
            );
            return -1;
        }
    }

    public static int setWaypointActualPos(CommandContext<CommandSourceStack> context) {
        return setDynamicWaypointPosImpl(context, (store, id, level, pos) -> {
            store.setActual(id, level, pos);
            return Command.SINGLE_SUCCESS;
        });
    }


    public static int setWaypointRenderPos(CommandContext<CommandSourceStack> context) {
        return setDynamicWaypointPosImpl(context, (store, id, level, pos) -> {
            store.setRendering(id, level, pos);
            return Command.SINGLE_SUCCESS;
        });
    }

    public static int setDynamicWaypointPosImpl(CommandContext<CommandSourceStack> context, Function4<DynamicLocationStorage, ResourceLocation, Level, BlockPos, Integer> handler) {
        final CommandSourceStack src = context.getSource();
        final Level level = src.getLevel();
        final BlockPos pos = context.getArgument("pos", BlockPos.class);
        final ResourceLocation id = context.getArgument("id", ResourceLocation.class);
        final Waypoint wp = SignMeUp.MANAGER.findWaypoint(id);
        if (wp != null) {
            if (wp.hasDynamicLocation()) {
                return level.getCapability(DynamicLocationStorage.CAP).map(store -> handler.apply(store, id, level, pos)).orElse(-1);
            } else {
                src.sendFailure(Component.literal("Error: waypoint " + id + " is static"));
                return -1;
            }
        } else {
            src.sendFailure(Component.literal("Error: waypoint " + id + " does not exist"));
            return -1;
        }
    }
}

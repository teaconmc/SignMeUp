package org.teacon.signin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Function4;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkDirection;
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
    public static final TranslationTextComponent ERROR = new TranslationTextComponent("sign_up.text.error");

    public static int listMaps(CommandContext<CommandSource> context) {
        CommandSource src = context.getSource();
        if (SignMeUp.MANAGER.getAllMaps().size() != 0) {
            src.sendFeedback(new TranslationTextComponent("sign_up.text.list_maps")
                    .appendString(": ")
                    , false);
            for (GuideMap map : SignMeUp.MANAGER.getAllMaps()) {
                src.sendFeedback(map.getTitle(), false);
            }
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendErrorMessage(ERROR
                    .appendString(": ")
                    .appendSibling(new TranslationTextComponent("sign_up.text.no_map_exists"))
            );
            return -1;
        }
    }

    public static int openSpecificMap(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandSource src = context.getSource();
        ServerPlayerEntity player = context.getSource().asPlayer();
        final ResourceLocation id = context.getArgument("id", ResourceLocation.class);
        GuideMap map = SignMeUp.MANAGER.findMap(id);
        if (map != null) {
            // Here we have to send a packet to client side
            // for rendering the map GUI
            SignMeUp.channel.sendTo(new MapScreenPacket(id), player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendErrorMessage(ERROR
                    .appendString(": ")
                    .appendSibling(new TranslationTextComponent("sign_up.text.map"))
                    .appendString(" " + id.toString() + " ")
                    .appendSibling(new TranslationTextComponent("sign_up.text.does_not_exist"))
            );
            return -1;
        }
    }

    public static int openNearestMap(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandSource src = context.getSource();
        Minecraft mc = Minecraft.getInstance();
        ServerPlayerEntity player = src.asPlayer();
        RegistryKey<World> worldKey = src.getWorld().getDimensionKey();
        GuideMap map = null;

        // We first check the dimension
        if (src.asPlayer().world.getDimensionKey() == worldKey) {
            // Then we look for the nearest in-range map
            for (GuideMap guideMap : SignMeUp.MANAGER.getAllMaps()) {
                final Vector3d destination = Vector3d.copyCenteredWithVerticalOffset(guideMap.center, player.getPosY());
                if (player.getPosition().withinDistance(destination, guideMap.range)) {
                    map = guideMap;
                    break; // Escape from the loop if we find one...
                }
            }
        }

        if (map != null) {
            // Same packet as above
            SignMeUp.channel.sendTo(new MapScreenPacket(SignMeUp.MANAGER.findMapId(map)), player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendErrorMessage(ERROR
                    .appendString(": ")
                    .appendSibling(new TranslationTextComponent("sign_up.status.no_map_available"))
            );
            return -1;
        }
    }

    public static int listWaypoints(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandSource src = context.getSource();
        ServerPlayerEntity player = src.asPlayer();
        if (SignMeUp.MANAGER.getAllWaypoints().size() != 0) {
            src.sendFeedback(new TranslationTextComponent("sign_up.text.list_points")
                    .appendString(": ")
                    , false);
            for (Waypoint waypoint : SignMeUp.MANAGER.getAllWaypoints()) {
                DecimalFormat df = new DecimalFormat("0.00");
                df.setRoundingMode(RoundingMode.HALF_UP);
                src.sendFeedback(new StringTextComponent(" - ")
                        .appendSibling(waypoint.getTitle()).appendString("\n   ")
                        .appendSibling(new TranslationTextComponent("sign_up.text.distance"))
                        .appendString(": " + df.format(waypoint.getActualLocation().distanceSq(player.getPosition())) + " ")
                        .appendSibling(new TranslationTextComponent("sign_up.text.blocks_away"))
                        , false
                );
            }
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendErrorMessage(ERROR.appendString(": ").appendSibling(new TranslationTextComponent("sign_up.text.no_waypoint_exists")));
            return -1;
        }
    }

    public static int listWaypointPos(CommandContext<CommandSource> context) {
        CommandSource src = context.getSource();
        if (SignMeUp.MANAGER.getAllWaypoints().size() != 0) {
            src.sendFeedback(new TranslationTextComponent("sign_up.text.list_points"), false);
            for (Waypoint waypoint : SignMeUp.MANAGER.getAllWaypoints()) {
                src.sendFeedback(new StringTextComponent(" - ")
                        .appendSibling(waypoint.getTitle())
                        .appendString("\n   ")
                        .appendSibling(new TranslationTextComponent("sign_up.text.render_location"))
                        .appendString(": ")
                        .appendString(waypoint.getRenderLocation().getCoordinatesAsString())
                        .appendString("\n   ")
                        .appendSibling(new TranslationTextComponent("sign_up.text.actual_location"))
                        .appendString(": ")
                        .appendString(waypoint.getActualLocation().getCoordinatesAsString())
                        , false
                );
            }
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendErrorMessage(ERROR.appendString(": ").appendSibling(new TranslationTextComponent("sign_up.text.no_waypoint_exists")));
            return -1;
        }
    }

    public static int getWaypointPos(CommandContext<CommandSource> context) {
        CommandSource src = context.getSource();
        final ResourceLocation id = context.getArgument("id", ResourceLocation.class);
        Waypoint waypoint = SignMeUp.MANAGER.findWaypoint(id);
        if (waypoint != null) {
            src.sendFeedback(new StringTextComponent(" - ")
                            .appendSibling(waypoint.getTitle())
                            .appendString("\n   ")
                            .appendSibling(new TranslationTextComponent("sign_up.text.render_location"))
                            .appendString(": ")
                            .appendString(waypoint.getRenderLocation().getCoordinatesAsString())
                            .appendString("\n   ")
                            .appendSibling(new TranslationTextComponent("sign_up.text.actual_location"))
                            .appendString(": ")
                            .appendString(waypoint.getActualLocation().getCoordinatesAsString())
                    , false
            );
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendErrorMessage(new StringTextComponent("Error: waypoint " + id + " does not exist"));
            src.sendErrorMessage(ERROR
                    .appendString(": ")
                    .appendSibling(new TranslationTextComponent("sign_up.text.waypoint")
                    .appendString(" " + id.toString() + " ")
                    .appendSibling(new TranslationTextComponent("sign_up.text.does_not_exist")))
            );
            return -1;
        }
    }

    public static int setWaypointActualPos(CommandContext<CommandSource> context) {
        return setDynamicWaypointPosImpl(context, (store, id, world, pos) -> {
            store.setActual(id, world, pos);
            return Command.SINGLE_SUCCESS;
        });
    }


    public static int setWaypointRenderPos(CommandContext<CommandSource> context) {
        return setDynamicWaypointPosImpl(context, (store, id, world, pos) -> {
            store.setRendering(id, world, pos);
            return Command.SINGLE_SUCCESS;
        });
    }

    public static int setDynamicWaypointPosImpl(CommandContext<CommandSource> context, Function4<DynamicLocationStorage, ResourceLocation, World, BlockPos, Integer> handler) {
        final CommandSource src = context.getSource();
        final World world = src.getWorld();
        final BlockPos pos = context.getArgument("pos", BlockPos.class);
        final ResourceLocation id = context.getArgument("id", ResourceLocation.class);
        final Waypoint wp = SignMeUp.MANAGER.findWaypoint(id);
        if (wp != null) {
            if (wp.hasDynamicLocation()) {
                return world.getCapability(DynamicLocationStorage.CAP).map(store -> handler.apply(store, id, world, pos)).orElse(-1);
            } else {
                src.sendErrorMessage(new StringTextComponent("Error: waypoint " + id + " is static"));
                return -1;
            }
        } else {
            src.sendErrorMessage(new StringTextComponent("Error: waypoint " + id + " does not exist"));
            return -1;
        }
    }
}

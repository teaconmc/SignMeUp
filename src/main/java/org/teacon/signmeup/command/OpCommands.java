package org.teacon.signmeup.command;

import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.network.NetworkHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.teacon.signmeup.command.argument.SpaceBreakStringArgumentType;
import org.teacon.signmeup.config.Waypoints;
import org.teacon.signmeup.network.RemoveWaypointPacket;
import org.teacon.signmeup.network.SetWaypointPacket;

import java.util.Optional;

/**
 * @author USS_Shenzhou
 */
public class OpCommands {

    public static void waypoints(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("smu")
                        .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("pos", Vec3Argument.vec3(false))
                                        .then(Commands.argument("rotation", RotationArgument.rotation())
                                                .then(Commands.argument("name", SpaceBreakStringArgumentType.string())
                                                        .then(Commands.argument("description", StringArgumentType.greedyString())
                                                                .executes(OpCommands::setWaypoint)
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", SpaceBreakStringArgumentType.string())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                ConfigHelper.getConfigRead(Waypoints.class).waypoints.stream().map(wayPoint -> wayPoint.name), builder))
                                        .executes(OpCommands::removeWaypoint)
                                )
                        )
        );
    }

    private static int setWaypoint(CommandContext<CommandSourceStack> context) {
        var pos = context.getArgument("pos", WorldCoordinates.class).getBlockPos(context.getSource());
        var name = context.getArgument("name", String.class);
        var description = context.getArgument("description", String.class);
        var rotation = context.getArgument("rotation", WorldCoordinates.class).getRotation(context.getSource());
        var waypoint = new Waypoints.WayPoint(name, description, pos.getX(), pos.getY(), pos.getZ(), rotation.y, rotation.x);

        ConfigHelper.getConfigWrite(Waypoints.class, waypoints -> {
            waypoints.waypoints.stream().filter(w -> w.name.equals(waypoint.name)).findFirst().ifPresentOrElse(
                    point -> context.getSource().sendSuccess(() -> Component.literal(point + " already exists. Remove it first if you want to replace it."), true),
                    () -> {
                        waypoints.waypoints.add(waypoint);
                        NetworkHelper.sendToAllPlayers(new SetWaypointPacket(name, description, pos, new Rotations(rotation.y, 0, rotation.x)));
                        context.getSource().sendSuccess(() -> Component.literal(waypoint + " has been added."), true);
                        if (description.startsWith("\"") && description.endsWith("\"")) {
                            context.getSource().sendSuccess(() -> Component.literal("The waypoint description is quoted with '\"'. This is a greedy string where '\"' is unnecessary.").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), true);
                        }
                    }
            );
        });

        return Command.SINGLE_SUCCESS;
    }

    private static int removeWaypoint(CommandContext<CommandSourceStack> context) {
        var name = context.getArgument("name", String.class);
        var waypoint = Waypoints.WayPoint.dumbWayPoint(name);
        ConfigHelper.getConfigWrite(Waypoints.class, waypoints -> {
            if (waypoints.waypoints.remove(waypoint)) {
                context.getSource().sendSuccess(() -> Component.literal(waypoint + " has been removed."), true);
                Optional.ofNullable(context.getSource().getPlayer())
                        .ifPresent(player -> NetworkHelper.sendToAllPlayers(new RemoveWaypointPacket(name)));
            } else {
                context.getSource().sendFailure(Component.literal("No waypoint called " + name));
            }
        });
        return Command.SINGLE_SUCCESS;
    }
}

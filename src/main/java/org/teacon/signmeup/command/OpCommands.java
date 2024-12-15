package org.teacon.signmeup.command;

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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.teacon.signmeup.command.argument.SpaceBreakStringArgumentType;
import org.teacon.signmeup.config.waypoints.Waypoint;
import org.teacon.signmeup.network.RemoveWaypointPacket;
import org.teacon.signmeup.network.SetWaypointPacket;

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
                                                Waypoint.INSTANCES.keySet(), builder
                                        )).executes(OpCommands::removeWaypoint)
                                )
                        )
        );
    }

    private static int setWaypoint(CommandContext<CommandSourceStack> context) {
        var pos = context.getArgument("pos", WorldCoordinates.class).getBlockPos(context.getSource());
        var name = context.getArgument("name", String.class);
        var description = context.getArgument("description", String.class);
        var rotation = context.getArgument("rotation", WorldCoordinates.class).getRotation(context.getSource());
        var waypoint = new Waypoint(name, description, pos.getX(), pos.getY(), pos.getZ(), rotation.y, rotation.x);

        Waypoint previous = Waypoint.INSTANCES.get(name);
        if (previous != null) {
            context.getSource().sendSuccess(() -> Component.literal(previous + " already exists. Remove it first if you want to replace it."), true);
        } else {
            Waypoint.INSTANCES.put(name, waypoint);
            NetworkHelper.sendToAllPlayers(new SetWaypointPacket(waypoint));

            context.getSource().sendSuccess(() -> Component.literal(waypoint + " has been added."), true);
            if (description.startsWith("\"") && description.endsWith("\"")) {
                context.getSource().sendSuccess(() -> Component.literal("The waypoint description is quoted with '\"'. This is a greedy string where '\"' is unnecessary.").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), true);
            }
        }

        Waypoint.save();

        return Command.SINGLE_SUCCESS;
    }

    private static int removeWaypoint(CommandContext<CommandSourceStack> context) {
        var name = context.getArgument("name", String.class);
        Waypoint waypoint = Waypoint.INSTANCES.remove(name);

        if (waypoint != null) {
            context.getSource().sendSuccess(() -> Component.literal(waypoint + " has been removed."), true);
            NetworkHelper.sendToAllPlayers(new RemoveWaypointPacket(name));
        } else {
            context.getSource().sendFailure(Component.literal("No waypoint called " + name));
        }

        Waypoint.save();
        return Command.SINGLE_SUCCESS;
    }
}

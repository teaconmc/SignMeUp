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
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.teacon.signmeup.command.argument.SpaceBreakStringArgumentType;
import org.teacon.signmeup.config.waypoints.Waypoint;
import org.teacon.signmeup.network.RemoveWaypointPacket;
import org.teacon.signmeup.network.SetWaypointPacket;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                                                Stream.concat(
                                                        Waypoint.INSTANCES.values().stream().map(Waypoint::name),
                                                        Waypoint.INSTANCES.keySet().stream().map(UUID::toString)
                                                ), builder
                                        )).executes(OpCommands::removeWaypoint)
                                )
                        )
        );
    }

    private static int setWaypoint(CommandContext<CommandSourceStack> context) {
        var pos = context.getArgument("pos", WorldCoordinates.class).getPosition(context.getSource());
        var name = context.getArgument("name", String.class);
        var description = context.getArgument("description", String.class);
        var rotation = context.getArgument("rotation", WorldCoordinates.class).getRotation(context.getSource());
        boolean major = name.startsWith("#");
        var waypoint = new Waypoint(
                UUID.randomUUID(), major ? name.substring(1) : name, description,
                new Vector3f((float) pos.x, (float) pos.y, (float) pos.z), new Vector2f(rotation.y, rotation.x),
                new Waypoint.State(major)
        );

        Waypoint.INSTANCES.put(waypoint.uuid(), waypoint);
        NetworkHelper.sendToAllPlayers(new SetWaypointPacket(waypoint));

        context.getSource().sendSuccess(() -> Component.literal(waypoint + " has been added."), true);
        if (description.startsWith("\"") && description.endsWith("\"")) {
            context.getSource().sendSuccess(() -> Component.literal("The waypoint description is quoted with '\"'. This is a greedy string where '\"' is unnecessary.").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), true);
        }
        Waypoint.save();

        return Command.SINGLE_SUCCESS;
    }

    private static int removeWaypoint(CommandContext<CommandSourceStack> context) {
        String name = context.getArgument("name", String.class);
        try {
            UUID uuid = UUID.fromString(name);
            Waypoint waypoint = Waypoint.INSTANCES.remove(uuid);
            if (waypoint != null) {
                context.getSource().sendSuccess(() -> Component.literal(waypoint + " has been removed."), true);
                NetworkHelper.sendToAllPlayers(new RemoveWaypointPacket(uuid));

                Waypoint.save();
            } else {
                context.getSource().sendFailure(Component.literal("No waypoint with uuid: " + name));
            }
        } catch (IllegalArgumentException e) {
            List<Waypoint> waypoints = Waypoint.INSTANCES.values().stream().filter(w -> w.name().equals(name)).toList();
            switch (waypoints.size()) {
                case 0 -> context.getSource().sendFailure(Component.literal("No waypoint with called: " + name));
                case 1 -> {
                    Waypoint waypoint = waypoints.getFirst();

                    Waypoint.INSTANCES.remove(waypoint.uuid());
                    Waypoint.save();
                    context.getSource().sendSuccess(() -> Component.literal(waypoint + " has been removed."), true);
                    NetworkHelper.sendToAllPlayers(new RemoveWaypointPacket(waypoint.uuid()));
                }
                default ->
                        context.getSource().sendFailure(Component.literal("Multiple candidates found: " + waypoints.stream()
                                .map(Waypoint::toString).collect(Collectors.joining(", ", "[", "]"))
                        ));
            }
        }

        return Command.SINGLE_SUCCESS;
    }
}

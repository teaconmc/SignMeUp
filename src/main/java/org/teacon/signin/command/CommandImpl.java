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
import org.teacon.signin.SignMeUp;
import org.teacon.signin.client.GuideMapScreen;
import org.teacon.signin.data.DynamicLocationStorage;
import org.teacon.signin.data.GuideMap;
import org.teacon.signin.data.Waypoint;

/**
 * This class contains command execution implementations
 */
public class CommandImpl {
    public static int listMaps(CommandContext<CommandSource> context) {
        CommandSource src = context.getSource();
        if (SignMeUp.MANAGER.getAllMaps().size() != 0) {
            for (GuideMap map : SignMeUp.MANAGER.getAllMaps()) {
                src.sendFeedback(new TranslationTextComponent("sign_up.text.list_maps"), false);
                src.sendFeedback(map.getTitle(), false);
            }
            return Command.SINGLE_SUCCESS;
        } else {
            return -1;
        }
    }

    public static int openSpecificMap(CommandContext<CommandSource> context) {
        CommandSource src = context.getSource();
        final ResourceLocation id = context.getArgument("id", ResourceLocation.class);
        GuideMap map = SignMeUp.MANAGER.findMap(id);
        if (map != null) {
            Minecraft.getInstance().displayGuiScreen(new GuideMapScreen(map));
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendErrorMessage(new StringTextComponent("Error: map " + id + " does not exist"));
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
            mc.displayGuiScreen(new GuideMapScreen(map));
            return Command.SINGLE_SUCCESS;
        } else {
            src.sendErrorMessage(new StringTextComponent("Error: No maps currently available :( You might be outside of map range"));
            return -1;
        }
    }

    public static int listWaypoints(CommandContext<CommandSource> context) {
        context.getSource().sendFeedback(new StringTextComponent("WIP :("), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int getWaypointPos(CommandContext<CommandSource> context) {
        context.getSource().sendFeedback(new StringTextComponent("WIP :("), false);
        return Command.SINGLE_SUCCESS;
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

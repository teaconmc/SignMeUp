package org.teacon.signin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.teacon.signin.client.SignMeUpClient;
import org.teacon.signin.command.CommandImpl;
import org.teacon.signin.data.DynamicLocationStorage;
import org.teacon.signin.data.GuideMapManager;
import org.teacon.signin.data.entity.Trigger;
import org.teacon.signin.network.*;

@Mod(SignMeUp.MODID)
@Mod.EventBusSubscriber(modid = SignMeUp.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SignMeUp {

    public static final String MODID = "sign_up";

    public static final GuideMapManager MANAGER = new GuideMapManager();

    public static SimpleChannel channel = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(SignMeUp.MODID, "data"))
            .networkProtocolVersion(() -> "0.0")
            .clientAcceptedVersions("0.0"::equals)
            .serverAcceptedVersions("0.0"::equals)
            .simpleChannel();

    public SignMeUp() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(SignMeUp::setup);
        MinecraftForge.EVENT_BUS.register(MANAGER);
    }

    public static void setup(FMLCommonSetupEvent event) {
        //CapabilityManager.INSTANCE.register(DynamicLocationStorage.class, new DynamicLocationStorage.Serializer(), DynamicLocationStorage::new);
        channel.registerMessage(0, SyncGuideMapPacket.class, SyncGuideMapPacket::write, SyncGuideMapPacket::new, SyncGuideMapPacket::handle);
        channel.registerMessage(1, PartialUpdatePacket.class, PartialUpdatePacket::write, PartialUpdatePacket::new, PartialUpdatePacket::handle);
        channel.registerMessage(2, MapScreenPacket.class, MapScreenPacket::write, MapScreenPacket::new, MapScreenPacket::handle);
        channel.registerMessage(3, TriggerFromMapPacket.class, TriggerFromMapPacket::write, TriggerFromMapPacket::new, TriggerFromMapPacket::handle);
        channel.registerMessage(4, TriggerFromWaypointPacket.class, TriggerFromWaypointPacket::write, TriggerFromWaypointPacket::new, TriggerFromWaypointPacket::handle);
    }

    @SubscribeEvent
    public void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(DynamicLocationStorage.class);
    }

    @SubscribeEvent
    public static void dataLoading(AddReloadListenerEvent event) {
        event.addListener(MANAGER);
    }

    @SubscribeEvent
    public static void command(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("signmeup")
                .then(Commands.literal("map")
                        .then(Commands.literal("list").executes(CommandImpl::listMaps))
                        .then(Commands.literal("close")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .executes(CommandImpl::closeSpecificMap))
                                .executes(CommandImpl::closeAnyMap))
                        .then(Commands.literal("open")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .executes(CommandImpl::openSpecificMap))
                                .executes(CommandImpl::openNearestMap)))
                .then(Commands.literal("point")
                        .then(Commands.literal("list")
                                .then(Commands.literal("location")
                                        .executes(CommandImpl::listWaypointPos))
                                .executes(CommandImpl::listWaypoints))
                        .then(Commands.literal("get")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .then(Commands.literal("location")
                                                .executes(CommandImpl::getWaypointPos))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .then(Commands.literal("actual")
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(CommandImpl::setWaypointActualPos)))
                                        .then(Commands.literal("render")
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(CommandImpl::setWaypointRenderPos))))))
                .then(Commands.literal("trigger")
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests((src, builder) -> {
                                    if (FMLEnvironment.dist.isClient()) {
                                        SignMeUpClient.MANAGER.getAllTriggers().forEach(location -> builder.suggest(location.toString()));
                                    } else {
                                        SignMeUp.MANAGER.getAllTriggers().forEach(location -> builder.suggest(location.toString()));
                                    }
                                    return builder.buildFuture();
                                }).executes(CommandImpl::trigger))));
    }

    @SubscribeEvent
    public static void attachCap(AttachCapabilitiesEvent<Level> event) {
        event.addCapability(new ResourceLocation("sign_up"), new DynamicLocationStorage.Holder());
    }

    public static boolean trigger(ServerPlayer player, Vec3i pos, ResourceLocation triggerId, boolean isCommand) {
        final Trigger trigger = MANAGER.findTrigger(triggerId);
        if (trigger != null && trigger.isVisibleTo(player)) {
            final MinecraftServer server = player.getServer();
            if (server != null) {
                final Vec3 pos3d = Vec3.atLowerCornerOf(pos);
                final CommandSourceStack source = isCommand
                        ? player.createCommandSourceStack().withPosition(pos3d).withPermission(2)
                        : player.createCommandSourceStack().withPosition(pos3d).withSuppressedOutput().withMaximumPermission(2);
                for (String command : trigger.executes) {
                    server.getCommands().performPrefixedCommand(source, command);
                }
            }
            return true;
        } else {
            // TODO: make it translatable
            player.displayClientMessage(Component.literal("You seemed to click the void just now..."), true);
            return false;
        }
    }
}

package org.teacon.signin;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.teacon.signin.command.CommandImpl;
import org.teacon.signin.data.DynamicLocationStorage;
import org.teacon.signin.data.GuideMapManager;
import org.teacon.signin.data.Trigger;
import org.teacon.signin.network.*;

@Mod("sign_up")
@Mod.EventBusSubscriber(modid = "sign_up", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SignMeUp {

    public static final GuideMapManager MANAGER = new GuideMapManager();

    public static SimpleChannel channel = NetworkRegistry.ChannelBuilder.named(new ResourceLocation("sign_up", "data"))
            .networkProtocolVersion(() -> "0.0")
            .clientAcceptedVersions("0.0"::equals)
            .serverAcceptedVersions("0.0"::equals)
            .simpleChannel();

    public SignMeUp() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(SignMeUp::setup);
        MinecraftForge.EVENT_BUS.register(MANAGER);
    }

    public static void setup(FMLCommonSetupEvent event) {
        CapabilityManager.INSTANCE.register(DynamicLocationStorage.class, new DynamicLocationStorage.Serializer(), DynamicLocationStorage::new);
        channel.registerMessage(0, SyncGuideMapPacket.class, SyncGuideMapPacket::write, SyncGuideMapPacket::new, SyncGuideMapPacket::handle);
        channel.registerMessage(1, PartialUpdatePacket.class, PartialUpdatePacket::write, PartialUpdatePacket::new, PartialUpdatePacket::handle);
        channel.registerMessage(2, MapScreenPacket.class, MapScreenPacket::write, MapScreenPacket::new, MapScreenPacket::handle);
        channel.registerMessage(3, TriggerFromMapPacket.class, TriggerFromMapPacket::write, TriggerFromMapPacket::new, TriggerFromMapPacket::handle);
        channel.registerMessage(4, TriggerFromWaypointPacket.class, TriggerFromWaypointPacket::write, TriggerFromWaypointPacket::new, TriggerFromWaypointPacket::handle);
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
                                .then(Commands.argument("id", ResourceLocationArgument.resourceLocation())
                                        .executes(CommandImpl::closeSpecificMap))
                                .executes(CommandImpl::closeAnyMap))
                        .then(Commands.literal("open")
                                .then(Commands.argument("id", ResourceLocationArgument.resourceLocation())
                                        .executes(CommandImpl::openSpecificMap))
                                .executes(CommandImpl::openNearestMap)))
                .then(Commands.literal("point")
                        .then(Commands.literal("list")
                                .then(Commands.literal("location")
                                        .executes(CommandImpl::listWaypointPos))
                                .executes(CommandImpl::listWaypoints))
                        .then(Commands.literal("get")
                                .then(Commands.argument("id", ResourceLocationArgument.resourceLocation())
                                        .then(Commands.literal("location")
                                                .executes(CommandImpl::getWaypointPos))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("id", ResourceLocationArgument.resourceLocation())
                                        .then(Commands.literal("actual")
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(CommandImpl::setWaypointActualPos)))
                                        .then(Commands.literal("render")
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(CommandImpl::setWaypointRenderPos)))))));
    }

    @SubscribeEvent
    public static void attachCap(AttachCapabilitiesEvent<World> event) {
        event.addCapability(new ResourceLocation("sign_up"), new DynamicLocationStorage.Holder());
    }

    public static void trigger(ServerPlayerEntity player, Vector3i pos, ResourceLocation triggerId) {
        final Trigger trigger = MANAGER.findTrigger(triggerId);
        if (trigger != null && trigger.isVisibleTo(player)) {
            final MinecraftServer server = player.getServer();
            if (server != null) {
                final CommandSource source = player.getCommandSource()
                        .withPos(Vector3d.copy(pos)).withFeedbackDisabled().withMinPermissionLevel(2);
                for (String command : trigger.executes) {
                    server.getCommandManager().handleCommand(source, command);
                }
            }
        } else {
            player.sendStatusMessage(new StringTextComponent("You seemed to click the void just now..."), true);
        }
    }
}

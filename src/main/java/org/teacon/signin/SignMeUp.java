package org.teacon.signin;

import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
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
import org.teacon.signin.data.*;
import org.teacon.signin.network.MapScreenPacket;
import org.teacon.signin.network.PartialUpdate;
import org.teacon.signin.network.SyncGuideMap;
import org.teacon.signin.network.TriggerActivation;

@Mod("sign_up")
@Mod.EventBusSubscriber(modid = "sign_up", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SignMeUp {

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
        channel.registerMessage(0, SyncGuideMap.class, SyncGuideMap::write, SyncGuideMap::new, SyncGuideMap::handle);
        channel.registerMessage(1, PartialUpdate.class, PartialUpdate::write, PartialUpdate::new, PartialUpdate::handle);
        channel.registerMessage(2, TriggerActivation.class, TriggerActivation::write, TriggerActivation::new, TriggerActivation::handle);
        channel.registerMessage(3, MapScreenPacket.class, MapScreenPacket::write, MapScreenPacket::new, MapScreenPacket::handle);
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
                        .then(Commands.literal("open")
                                .then(Commands.argument("id", ResourceLocationArgument.resourceLocation())
                                        .executes(CommandImpl::openSpecificMap))
                                .executes(CommandImpl::openNearestMap)))
                .then(Commands.literal("point")
                        .then(Commands.literal("list").executes(CommandImpl::listWaypoints))
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

    public static void trigger(ServerPlayerEntity player, ResourceLocation triggerId) {
        final Trigger trigger = MANAGER.findTrigger(triggerId);
        if (trigger != null && trigger.isVisibleTo(player)) {
            final MinecraftServer server = player.getServer();
            if (server != null) {
                server.getCommandManager().handleCommand(player.getCommandSource().withPermissionLevel(2), trigger.command);
            }
        } else {
            player.sendStatusMessage(new StringTextComponent("You seemed to click the void just now..."), true);
        }
    }
}

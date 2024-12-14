package org.teacon.signmeup;

import cn.ussshenzhou.t88.config.ConfigHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.teacon.signmeup.command.argument.ModCommandArgumentRegistry;
import org.teacon.signmeup.config.Map;
import org.teacon.signmeup.config.MiniMap;
import org.teacon.signmeup.config.PlayerCommands;
import org.teacon.signmeup.config.Waypoints;

/**
 * @author USS_Shenzhou
 */
@Mod(SignMeUp.MODID)
public class SignMeUp {
    public static final String MODID = "sign_up";

    public static final boolean SODIUM_INSTALLED = ModList.get().isLoaded("sodium");
    public static final boolean IRIS_INSTALLED = ModList.get().isLoaded("iris");

    public SignMeUp(IEventBus bus, ModContainer mod) {
        ModCommandArgumentRegistry.register(bus);

        ConfigHelper.loadConfig(new PlayerCommands());
        ConfigHelper.loadConfig(new Map());
        ConfigHelper.loadConfig(new Waypoints());
        ConfigHelper.loadConfig(new MiniMap());

        Minecraft.getInstance().execute(() -> Thread.currentThread().setPriority((Thread.MAX_PRIORITY + Thread.NORM_PRIORITY) / 2));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

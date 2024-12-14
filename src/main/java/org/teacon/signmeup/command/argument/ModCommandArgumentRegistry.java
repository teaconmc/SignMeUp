package org.teacon.signmeup.command.argument;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.teacon.signmeup.SignMeUp;

import java.util.function.Supplier;

public class ModCommandArgumentRegistry {
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, SignMeUp.MODID);

    public static final Supplier<ArgumentTypeInfo<?, ?>> SPACE_BREAK_STRING_ARGUMENT = REGISTRY.register("space_break_string", () -> ArgumentTypeInfos.registerByClass(SpaceBreakStringArgumentType.class, new SpaceBreakStringArgumentType.Serializer()));

    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
    }
}

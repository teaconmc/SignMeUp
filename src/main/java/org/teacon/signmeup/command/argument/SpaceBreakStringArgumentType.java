package org.teacon.signmeup.command.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class SpaceBreakStringArgumentType implements ArgumentType<String> {
    public static final Pattern NON_SPACE_PATTERN = Pattern.compile("^\\S+");

    public static SpaceBreakStringArgumentType string() {
        return new SpaceBreakStringArgumentType();
    }

    private SpaceBreakStringArgumentType() {
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        final var next = reader.peek();
        if (StringReader.isQuotedStringStart(next)) {
            return reader.readString();
        }

        final var text = reader.getRemaining();
        var matcher = NON_SPACE_PATTERN.matcher(text);
        if (matcher.find()) {
            reader.setCursor(reader.getCursor() + matcher.end());
            return matcher.group();
        }
        return "";
    }

    @Override
    public String toString() {
        return "spaceBreakString()";
    }

    public static class Serializer implements ArgumentTypeInfo<SpaceBreakStringArgumentType, Serializer.Template> {
        @Override
        public void serializeToNetwork(@NotNull Template template, @NotNull FriendlyByteBuf buf) {
        }

        @Override
        public @NotNull Template deserializeFromNetwork(@NotNull FriendlyByteBuf buf) {
            return new Template();
        }

        @Override
        public void serializeToJson(@NotNull Template template, @NotNull JsonObject jsonObject) {
        }

        @Override
        public @NotNull Template unpack(@NotNull SpaceBreakStringArgumentType type) {
            return new Template();
        }

        public class Template implements ArgumentTypeInfo.Template<SpaceBreakStringArgumentType> {

            @Override
            public @NotNull SpaceBreakStringArgumentType instantiate(@NotNull CommandBuildContext context) {
                return SpaceBreakStringArgumentType.string();
            }

            @Override
            public @NotNull ArgumentTypeInfo<SpaceBreakStringArgumentType, ?> type() {
                return Serializer.this;
            }
        }
    }
}

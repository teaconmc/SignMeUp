/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package org.teacon.exhibition_portal.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.server.command.CommandUtils;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EnumStringArgument implements ArgumentType<String> {
    private static final Dynamic2CommandExceptionType INVALID_ENUM = new Dynamic2CommandExceptionType(
            (found, expected) -> CommandUtils.makeTranslatableWithFallback("commands.exhibition_portal.invalid_enum_string", found, expected)
    );

    private final List<String> values;

    public EnumStringArgument(String... values) {
        this.values = List.of(values);
    }

    public EnumStringArgument(List<String> values) {
        this.values = values;
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        String name = reader.readUnquotedString();
        if (!values.contains(name)) {
            throw INVALID_ENUM.createWithContext(reader, name, String.join(",", values));

        }
        return name;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(values.stream(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return values;
    }

    public static class Info implements ArgumentTypeInfo<EnumStringArgument, Info.Template> {
        private static final StreamCodec<ByteBuf, List<String>> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

        @Override
        public void serializeToNetwork(Template template, @NonNull FriendlyByteBuf buffer) {
            STREAM_CODEC.encode(buffer, template.values);
        }

        @Override
        public @NonNull Template deserializeFromNetwork(@NonNull FriendlyByteBuf buffer) {
            return new Template(STREAM_CODEC.decode(buffer));
        }

        @Override
        public void serializeToJson(Template template, @NonNull JsonObject json) {
            JsonArray array = new JsonArray();
            for (String value : template.values) {
                array.add(value);
            }
            json.add("enum", array);
        }

        @Override
        public @NonNull Template unpack(EnumStringArgument argument) {
            return new Template(argument.values);
        }

        public class Template implements ArgumentTypeInfo.Template<EnumStringArgument> {
            private final List<String> values;

            Template(List<String> values) {
                this.values = values;
            }

            @Override
            public @NonNull EnumStringArgument instantiate(@NonNull CommandBuildContext structure) {
                return new EnumStringArgument(this.values);
            }

            @Override
            public @NonNull ArgumentTypeInfo<EnumStringArgument, ?> type() {
                return Info.this;
            }
        }
    }
}

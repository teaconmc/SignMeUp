package org.teacon.signin.data;

import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.Util;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.signin.network.PartialUpdatePacket;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class Trigger implements PlayerTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(Trigger.class);

    Component title;
    Component desc;

    public volatile boolean disabled = false;

    String selector = "@e";
    private transient EntitySelector parsedSelector;
    private transient boolean invalid = false;

    public ImmutableList<String> executes = ImmutableList.of();

    transient Set<ServerPlayer> visiblePlayers = Collections.newSetFromMap(new WeakHashMap<>());

    public Component getTitle() {
        return this.title == null ? Component.translatable("sign_up.trigger.unnamed") : this.title;
    }

    public Component getDesc() {
        return this.desc == null ? this.getTitle() : this.desc;
    }

    @Override
    public EntitySelector getSelector() {
        if (this.parsedSelector == null && !this.invalid) {
            try {
                this.parsedSelector = new EntitySelectorParser(new StringReader(this.selector)).parse();
            } catch (CommandSyntaxException e) {
                LOGGER.warn("Invalid selector: {}", this.selector);
                this.invalid = true;
                return null;
            }
        }
        return this.parsedSelector;
    }

    public boolean isVisibleTo(ServerPlayer p) {
        return this.visiblePlayers.contains(p);
    }

    @Override
    public Set<ServerPlayer> getTracking() {
        return this.visiblePlayers;
    }

    @Override
    public void setTracking(Set<ServerPlayer> players) {
        this.visiblePlayers = players;
    }

    @Override
    public PartialUpdatePacket getNotifyPacket(boolean remove, ResourceLocation id) {
        return new PartialUpdatePacket(remove ? PartialUpdatePacket.Mode.REMOVE_TRIGGER : PartialUpdatePacket.Mode.ADD_TRIGGER, id, this);
    }

    public static final class Serializer implements JsonDeserializer<Trigger>, JsonSerializer<Trigger> {

        @Override
        public Trigger deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonObject()) {
                final JsonObject obj = json.getAsJsonObject();
                final Trigger t = new Trigger();
                if (obj.has("title")) {
                    t.title = context.deserialize(obj.get("title"), Component.class);
                }
                if (obj.has("description")) {
                    t.desc = context.deserialize(obj.get("description"), Component.class);
                }
                if (obj.has("disabled")) {
                    t.disabled = obj.get("disabled").getAsBoolean();
                }
                if (obj.has("selector")) {
                    t.selector = obj.get("selector").getAsString();
                }
                if (obj.has("executes")) {
                    ImmutableList.Builder<String> builder = ImmutableList.builder();
                    for (JsonElement command : obj.getAsJsonArray("executes")) {
                        builder.add(command.getAsString());
                    }
                    t.executes = builder.build();
                } else {
                    t.executes = t.disabled ? ImmutableList.of() : ImmutableList.of("/me successfully created a new trigger");
                }
                return t;
            } else {
                throw new JsonParseException("Trigger must be a JSON Object");
            }
        }

        @Override
        public JsonElement serialize(Trigger src, Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject json = new JsonObject();
            if (src.title != null) {
                json.add("title", context.serialize(src.title));
            }
            if (src.desc != null) {
                json.add("description", context.serialize(src.desc));
            }
            json.add("disabled", new JsonPrimitive(src.disabled));
            json.add("selector", new JsonPrimitive(src.selector));
            if (src.executes.isEmpty() && src.disabled) {
                json.add("executes", Util.make(new JsonArray(), arr -> src.executes.forEach(arr::add)));
            }
            return json;
        }
    }
}

package org.teacon.signin.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.arguments.EntitySelector;
import net.minecraft.command.arguments.EntitySelectorParser;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class Trigger {

    ITextComponent title;
    // TODO Should be named 'description'
    ITextComponent desc;

    public volatile boolean disabled = false;

    String selector = "@e";
    private transient EntitySelector parsedSelector;

    public String command = "/me successfully created a new trigger";

    transient Set<ServerPlayerEntity> visiblePlayers = Collections.newSetFromMap(new WeakHashMap<>());

    public ITextComponent getTitle() {
        return this.title == null ? new TranslationTextComponent("sign_me_in.trigger.unnamed") : this.title;
    }

    public ITextComponent getDesc() {
        return this.desc == null ? this.getTitle() : this.desc;
    }

    public EntitySelector getSelector() {
        if (parsedSelector == null) {
            try {
                parsedSelector = new EntitySelectorParser(new StringReader(this.selector)).parse();
            } catch (CommandSyntaxException e) {
                return null;
            }
        }
        return parsedSelector;
    }

    public static final class Serializer implements JsonDeserializer<Trigger>, JsonSerializer<Trigger> {

        @Override
        public Trigger deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonObject()) {
                final JsonObject obj = json.getAsJsonObject();
                final Trigger t = new Trigger();
                if (obj.has("title")) {
                    t.title = context.deserialize(obj.get("title"), ITextComponent.class);
                }
                if (obj.has("description")) {
                    t.title = context.deserialize(obj.get("description"), ITextComponent.class);
                }
                if (obj.has("disabled")) {
                    t.disabled = obj.get("disabled").getAsBoolean();
                }
                if (obj.has("selector")) {
                    t.selector = obj.get("selector").getAsString();
                }
                if (obj.has("command")) {
                    t.selector = obj.get("command").getAsString();
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
            json.add("command", new JsonPrimitive(src.command));
            return json;
        }
    }
}

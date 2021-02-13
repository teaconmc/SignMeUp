package org.teacon.signin.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.arguments.EntitySelector;
import net.minecraft.command.arguments.EntitySelectorParser;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class Trigger {

    public ITextComponent title;
    // TODO Should be named 'description'
    public ITextComponent desc;

    public volatile boolean disabled = false;

    public String selector = "@e";
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
            return null;
        }

        @Override
        public JsonElement serialize(Trigger src, Type typeOfSrc, JsonSerializationContext context) {
            return null;
        }
    }
}

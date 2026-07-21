package org.teacon.exhibition_portal.mixin;

import it.unimi.dsi.fastutil.ints.IntArrays;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.StringSplitter$LineBreakFinder")
public class StringSplitter_LineBreakFinderMixin {
    @Shadow
    private int lastSpace;

    @Shadow
    private int offset;

    @Shadow
    private Style lastSpaceStyle;

    @Unique
    private static final int[] BLACK_LIST = "。，、；：？！”】）》—…~·".codePoints().sorted().toArray();

//    @Inject(method = "accept", at = @At(value = "RETURN", ordinal = 1))
//    private void beforeAccept(int position, Style style, int codepoint, CallbackInfoReturnable<Boolean> cir) {
//        if (codepoint != 32 && Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN && IntArrays.binarySearch(BLACK_LIST, codepoint) < 0) {
//            this.lastSpace = position + this.offset;
//            this.lastSpaceStyle = style;
//        }
//    }
}

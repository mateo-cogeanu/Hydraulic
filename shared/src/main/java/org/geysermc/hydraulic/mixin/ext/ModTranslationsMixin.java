package org.geysermc.hydraulic.mixin.ext;

import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.hydraulic.text.ModTranslations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MinecraftLocale.class, remap = false)
public class ModTranslationsMixin {
    @Inject(method = "getLocaleStringIfPresent", at = @At("HEAD"), cancellable = true)
    private static void hydraulic$translateMod(String key, String locale, CallbackInfoReturnable<String> cir) {
        String translation = ModTranslations.translate(key, locale);
        if (translation != null) cir.setReturnValue(translation);
    }
}

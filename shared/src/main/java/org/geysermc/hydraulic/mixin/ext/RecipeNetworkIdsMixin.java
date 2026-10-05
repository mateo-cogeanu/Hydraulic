package org.geysermc.hydraulic.mixin.ext;

import org.geysermc.geyser.inventory.recipe.RecipeUtil;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.hydraulic.compat.RecipeNetworkIds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GeyserSession.class, remap = false)
public class RecipeNetworkIdsMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void hydraulic$reserveMapRecipeIds(CallbackInfo ci) {
        // Trigger map recipe registration before this session allocates trims/stonecutter IDs.
        RecipeUtil.CARTOGRAPHY_RECIPES.size();
        RecipeNetworkIds.reserve(((GeyserSession) (Object) this).getLastRecipeNetId(), InventoryUtils.LAST_RECIPE_NET_ID);
    }
}

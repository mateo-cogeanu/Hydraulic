package org.geysermc.hydraulic.mixin.ext;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.geysermc.hydraulic.block.JavaBlockStateRemapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "org.geysermc.geyser.platform.mod.world.GeyserModWorldManager", remap = false)
public class WorldBlockStatesMixin {
    // Intercept only native world reads; chunk-cache fallbacks are already translated.
    @ModifyExpressionValue(method = "getBlockAt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;getId(Lnet/minecraft/world/level/block/state/BlockState;)I"))
    private int hydraulic$remapWorldState(int serverId) {
        return JavaBlockStateRemapper.translate(serverId);
    }
}

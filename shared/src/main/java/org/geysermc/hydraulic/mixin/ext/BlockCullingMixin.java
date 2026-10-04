package org.geysermc.hydraulic.mixin.ext;

import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.registry.populator.CustomBlockRegistryPopulator;
import org.geysermc.hydraulic.block.StructureGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hide buried faces of Hydraulic's explicit solid cubes. */
@Mixin(value = CustomBlockRegistryPopulator.class, remap = false)
public class BlockCullingMixin {
    @Inject(method = "convertComponents", at = @At("RETURN"), cancellable = true)
    private static void hydraulic$cullSolidFaces(CustomBlockComponents components, CallbackInfoReturnable<NbtMap> cir) {
        if (components == null || components.geometry() == null) return;
        String id = components.geometry().identifier();
        if (!id.equals(StructureGeometry.IDENTIFIER) && !id.equals("geometry.hydraulic.sift_portal")) return;
        NbtMap result = cir.getReturnValue();
        NbtMap geometry = result.getCompound("minecraft:geometry").toBuilder()
                .putString("culling", StructureGeometry.CULLING).build();
        cir.setReturnValue(result.toBuilder().putCompound("minecraft:geometry", geometry).build());
    }
}

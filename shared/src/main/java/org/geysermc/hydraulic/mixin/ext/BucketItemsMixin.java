package org.geysermc.hydraulic.mixin.ext;

import java.util.List;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.hydraulic.item.ItemPackModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Route custom fluid buckets through Geyser's native raycast/use-item path. */
@Mixin(value = ItemMappings.class, remap = false)
public class BucketItemsMixin {
    @Inject(method = "getBuckets", at = @At("RETURN"))
    private void hydraulic$customBuckets(CallbackInfoReturnable<List<ItemDefinition>> callback) {
        List<ItemDefinition> buckets = callback.getReturnValue();
        synchronized (buckets) {
            ItemMappings mappings = (ItemMappings) (Object) this;
            for (int id : ItemPackModule.BUCKET_ITEM_IDS) {
                var mapping = mappings.getMapping(id);
                if (mapping != null && !buckets.contains(mapping.getBedrockDefinition())) buckets.add(mapping.getBedrockDefinition());
            }
        }
    }
}

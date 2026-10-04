package org.geysermc.hydraulic.fabric.test.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import org.geysermc.hydraulic.fabric.test.ModBlocks;
import org.geysermc.hydraulic.fabric.test.ModItems;

import java.util.concurrent.CompletableFuture;

public class TagGeneration {
    public static class Blocks extends FabricTagsProvider.BlockTagsProvider {
        public Blocks(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.GOLDEN_BARREL.properties().blockId());

            tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.GOLDEN_BARREL.properties().blockId());
        }
    }

    public static class Items extends FabricTagsProvider.ItemTagsProvider {
        public Items(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(ItemTags.SWORDS).add(ModItems.BARREL_SWORD.builtInRegistryHolder().key());
            tag(ItemTags.PICKAXES).add(ModItems.BARREL_PICKAXE.builtInRegistryHolder().key());
            tag(ItemTags.AXES).add(ModItems.BARREL_AXE.builtInRegistryHolder().key());
            tag(ItemTags.SHOVELS).add(ModItems.BARREL_SHOVEL.builtInRegistryHolder().key());
            tag(ItemTags.HOES).add(ModItems.BARREL_HOE.builtInRegistryHolder().key());
        }
    }
}

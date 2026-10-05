package org.geysermc.hydraulic.item;

import com.google.auto.service.AutoService;
import net.kyori.adventure.key.Key;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserBlockPlacer;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserChargeable;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserItemDataComponents;
import org.geysermc.hydraulic.pack.PackLogListener;
import org.geysermc.hydraulic.pack.PackModule;
import org.geysermc.hydraulic.pack.TexturePackModule;
import org.geysermc.hydraulic.pack.context.PackEventContext;
import org.geysermc.hydraulic.pack.context.PackPostProcessContext;
import org.geysermc.hydraulic.pack.context.PackPreProcessContext;
import org.geysermc.hydraulic.component.ComponentConverter;
import org.geysermc.hydraulic.util.HydraulicKey;
import org.geysermc.hydraulic.util.PackUtil;
import org.geysermc.pack.bedrock.resource.BedrockResourcePack;
import org.geysermc.pack.converter.type.model.ModelStitcher;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.item.*;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTexture;

import java.util.*;

@AutoService(PackModule.class)
public class ItemPackModule extends TexturePackModule<ItemPackModule> {
    public static final Set<Integer> BUCKET_ITEM_IDS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<Key, ItemModel> itemModels = new HashMap<>();
    private final Set<Identifier> itemsWith2dIcon = new LinkedHashSet<>();
    private final Set<Identifier> handheldItems = new LinkedHashSet<>();
    private final Map<String, String> itemBuiltinTexture = new HashMap<>();

    public ItemPackModule() {
        this.listenOn(GeyserDefineCustomItemsEvent.class, this::onDefineCustomItems);

        this.preProcess(this::preProcess);
        this.postProcess(this::postProcess);
    }

    private void handleModel(@NotNull PackPreProcessContext<ItemPackModule> context, ItemModel itemModel, Identifier itemLocation) {
        ReferenceItemModel referenceModel = ItemModelResolver.reference(itemModel);
        if (referenceModel == null) return;
        Model model = context.modelProvider().model(referenceModel.model());
        if (model == null) return;
        List<Key> parents = PackUtil.modelParents(context.modelProvider(), model);
        if (parents.contains(Model.ITEM_HANDHELD)) {
            itemsWith2dIcon.add(itemLocation);
            handheldItems.add(itemLocation);
        } else if (parents.contains(Model.ITEM_GENERATED) || parents.contains(Model.BUILT_IN_GENERATED)) {
            itemsWith2dIcon.add(itemLocation);
        }
    }

    private Key modelKey(Item item, Identifier itemLocation) {
        Identifier definitionId = item.components().get(DataComponents.ITEM_MODEL);
        ItemModel definition = definitionId == null ? null : itemModels.get(Key.key(definitionId.toString()));
        ReferenceItemModel reference = ItemModelResolver.reference(definition);
        return reference == null ? Key.key(itemLocation.getNamespace(), "item/" + itemLocation.getPath()) : reference.model();
    }

    private void preProcess(@NotNull PackPreProcessContext<ItemPackModule> context) {
        for (team.unnamed.creative.item.Item item : context.assets(ResourcePack::items)) {
            itemModels.put(item.key(), item.model());
        }

        List<Item> items = context.registryValues(BuiltInRegistries.ITEM);
        PackLogListener packLogListener = new PackLogListener(context.logger());
        for (Item item : items) {
            Identifier itemLocation = BuiltInRegistries.ITEM.getKey(item);

            Identifier definitionId = item.components().get(DataComponents.ITEM_MODEL);
            handleModel(context, definitionId == null ? null : itemModels.get(Key.key(definitionId.toString())), itemLocation);
            Model baseModel = context.modelProvider().model(modelKey(item, itemLocation));
            if (baseModel == null) {
                continue;
            }

            Model model = new ModelStitcher(context.modelProvider(), baseModel, packLogListener).stitch();
            if (model == null) {
                continue;
            }
            List<ModelTexture> layers = model.textures().layers();
            if (layers == null || layers.isEmpty()) {
                continue;
            }

            Key layer0 = layers.getFirst().key();

            if (layer0 != null && layer0.namespace().equals(Key.MINECRAFT_NAMESPACE)) {
                itemBuiltinTexture.put(itemLocation.toString(), PackUtil.getTextureName(layer0.toString()));
            }
        }
    }

    private void postProcess(@NotNull PackPostProcessContext<ItemPackModule> context) {
        BedrockResourcePack bedrockPack = context.bedrockResourcePack();

        List<Item> items = context.registryValues(BuiltInRegistries.ITEM);

        context.logger().info("Items to convert: {} in mod {}", items.size(), context.mod().id());

        PackLogListener packLogListener = new PackLogListener(context.logger());
        for (Item item : items) {
            Identifier itemLocation = BuiltInRegistries.ITEM.getKey(item);

            Model baseModel = context.modelProvider().model(modelKey(item, itemLocation));
            if (baseModel == null) {
                context.logger().warn("Item {} has no item model, skipping", itemLocation);
                continue;
            }

            Model model = new ModelStitcher(context.modelProvider(), baseModel, packLogListener).stitch();

            if (model == null) continue;
            List<ModelTexture> layers = model.textures().layers();
            if (layers == null || layers.isEmpty()) {
                // Don't warn if a block as they can use the block model
                if (!(item instanceof BlockItem)) {
                    context.logger().warn("Item {} has no layer0 texture, skipping", itemLocation);
                }

                continue;
            }

            ModelTexture layer0 = layers.getFirst();
            if (layer0.key() == null) {
                context.logger().warn("Item {} has an unresolved layer0 texture", itemLocation);
                continue;
            }
            String outputLoc = getOutputFromModel(context, layer0.key());
            bedrockPack.addItemTexture(itemLocation.toString(), outputLoc.replace(".png", ""));
        }
    }

    @Override
    public boolean test(@NotNull PackPostProcessContext<ItemPackModule> context) {
        return !context.registryValues(BuiltInRegistries.ITEM).isEmpty();
    }

    private void onDefineCustomItems(PackEventContext<GeyserDefineCustomItemsEvent, ItemPackModule> context) {
        GeyserDefineCustomItemsEvent event = context.event();
        List<Item> items = context.registryValues(BuiltInRegistries.ITEM);

        DefaultedRegistry<Item> registry = BuiltInRegistries.ITEM;
        for (Item item : items) {
            Identifier itemLocation = registry.getKey(item);

            try {
                NonVanillaCustomItemDefinition.Builder customItemDefinition = NonVanillaCustomItemDefinition.builder(
                        org.geysermc.geyser.api.util.Identifier.of(itemLocation.toString()),
                        org.geysermc.geyser.api.util.Identifier.of(itemLocation.toString()),
                        registry.getId(item)
                )
                        .displayName("%" + item.getDescriptionId());

                CustomItemBedrockOptions.Builder customItemOptions = CustomItemBedrockOptions.builder()
                        .allowOffhand(true);

                // Allow minecraft namespace texture to be used (remapped as hydraulic)
                if (itemBuiltinTexture.containsKey(itemLocation.toString())) {
                    customItemOptions.icon(itemBuiltinTexture.get(itemLocation.toString()));
                }

                // Add the icon if it should have an icon
                boolean is2d = itemsWith2dIcon.contains(itemLocation);
                if (is2d) {
                    customItemOptions.icon(itemLocation.toString());
                }

                // Make it handheld if need be
                if (handheldItems.contains(itemLocation)) {
                    customItemOptions.displayHandheld(true);
                }

                // Set the creative mappings
                CreativeMappings.setup(item, customItemOptions);

                // Set all bedrock components using what java components we have
                ComponentConverter.setGeyserComponents(
                        item.components(),
                        customItemDefinition,
                        customItemOptions
                );

                // Set the needed component for bows to work correctly
                if (item instanceof BowItem) {
                    customItemDefinition.component(
                            GeyserItemDataComponents.CHARGEABLE,
                            GeyserChargeable.builder()
                                    .maxDrawDuration(1f)
                                    .chargeOnDraw(false)
                    );

                    // Include the default icon, this won't change in the hotbar when used but this works the best for now
                    customItemOptions.icon(itemLocation.toString());
                }

                // Set the needed component for crossbows to work correctly
                if (item instanceof CrossbowItem) {
                    customItemDefinition.component(
                            GeyserItemDataComponents.CHARGEABLE,
                            GeyserChargeable.builder()
                                    .maxDrawDuration(0f)
                                    .chargeOnDraw(true)
                    );

                    // Include the default icon, this won't change in the hotbar when used but this works the best for now
                    customItemOptions.icon(itemLocation.toString());
                }

                if (item instanceof BlockItem blockItem) {
                    // Set the block_placer component to the correct block
                    // This fixes animations sometimes not showing
                    Block block = blockItem.getBlock();

                    customItemDefinition.component(
                            GeyserItemDataComponents.BLOCK_PLACER,
                            GeyserBlockPlacer.of(HydraulicKey.of(BuiltInRegistries.BLOCK.getKey(block)), !is2d)
                    );

                    CreativeMappings.setupBlock(block, customItemOptions);
                }

                customItemDefinition.bedrockOptions(customItemOptions);

                if (item instanceof BucketItem) BUCKET_ITEM_IDS.add(registry.getId(item));
                event.register(customItemDefinition.build());
            } catch (Exception e) {
                context.logger().error("Unable to register {}:", itemLocation, e);
            }
        }
    }
}

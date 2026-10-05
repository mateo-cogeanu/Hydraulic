package org.geysermc.hydraulic.block;

import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import org.geysermc.geyser.api.block.custom.*;
import org.geysermc.geyser.api.block.custom.component.*;
import org.geysermc.geyser.api.block.custom.nonvanilla.JavaBlockState;
import org.geysermc.geyser.api.block.custom.nonvanilla.JavaBoundingBox;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.hydraulic.pack.PackModule;

/** Original Ichor animation on the first layer; water on the fluid layer preserves swimming. */
@AutoService(PackModule.class)
public class IchorPresentation extends PackModule<IchorPresentation> {
    public static final String PREFIX = "geometry.hydraulic.ichor_";
    private static final Gson GSON = new Gson();
    public static float height(int level) { return (level==0 || level>=8 ? 8 : 8-level)/9f; }
    public static JsonObject geometry(int level) {
        Map<String,Object> uv = new LinkedHashMap<>();
        for(String face:List.of("north","south","east","west","up","down")) uv.put(face,Map.of("uv",List.of(0,0),"uv_size",List.of(16,16),"material_instance",face));
        return GSON.toJsonTree(Map.of("format_version","1.16.0","minecraft:geometry",List.of(Map.of(
                "description",Map.of("identifier",PREFIX+level,"texture_width",16,"texture_height",16),"bones",List.of(Map.of(
                        "name","fluid","pivot",List.of(0,0,0),"cubes",List.of(Map.of("origin",List.of(-8,0,-8),"size",List.of(16,16*(height(level)+0.001f),16),"uv",uv)))))))).getAsJsonObject();
    }
    private static CustomBlockComponents components(int level) {
        var components=CustomBlockComponents.builder().geometry(GeometryComponent.builder().identifier(PREFIX+level).build())
                .collisionBox(BoxComponent.emptyBox()).selectionBox(BoxComponent.emptyBox()).lightDampening(1);
        for(String face:List.of("*","north","south","east","west","up","down")) components.materialInstance(face,
                MaterialInstance.builder().texture(face.equals("up")||face.equals("*")?"the_sift:ichor_still":"the_sift:ichor_flow")
                        .renderMethod("blend").faceDimming(false).ambientOcclusion(false).build());
        return components.build();
    }
    public IchorPresentation() {
        listenOn(GeyserDefineCustomBlocksEvent.class,context->{
            if(!context.mod().namespace().equals("the_sift"))return;
            var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("the_sift:ichor"));
            if(!(block instanceof LiquidBlock))return;
            var permutations=new ArrayList<CustomBlockPermutation>();
            for(int level=0;level<16;level++) permutations.add(new CustomBlockPermutation(components(level),"(query.block_property('level') == "+level+")"));
            var data=NonVanillaCustomBlockData.builder().namespace("the_sift").name("ichor").includedInCreativeInventory(false)
                    .intProperty("level",java.util.stream.IntStream.range(0,16).boxed().toList()).components(components(0)).permutations(permutations).build();
            context.event().register(data);
            for(var state:block.getStateDefinition().getPossibleStates()) {
                int level=state.getValue(LiquidBlock.LEVEL);
                IchorFluidLayer.LEVELS.put(Block.getId(state),level);
                context.event().registerOverride(JavaBlockState.builder().identifier(BlockStateParser.serialize(state)).javaId(Block.getId(state))
                        .stateGroupId(BuiltInRegistries.BLOCK.getId(block)).waterlogged(true).collision(new JavaBoundingBox[0])
                        .blockHardness(100).canBreakWithHand(false).pistonBehavior("DESTROY").build(),data.blockStateBuilder().intProperty("level",level).build());
            }
        });
        postProcess(context->{
            if(!context.mod().namespace().equals("the_sift"))return;
            var pack=context.bedrockResourcePack();
            for(int level=0;level<16;level++) {
                var model=geometry(level); pack.addExtraFile(model,"models/blocks/ichor_"+level+".json");
                // Only boundary side/bottom faces can occlude another fluid cell.
                var rule=GeometryCulling.definition(model.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject());
                pack.addExtraFile(rule,"block_culling/ichor_"+level+".json");
            }

        });
    }
}

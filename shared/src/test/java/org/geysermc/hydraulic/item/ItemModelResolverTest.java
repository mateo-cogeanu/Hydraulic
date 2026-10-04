package org.geysermc.hydraulic.item;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;
import team.unnamed.creative.item.ItemModel;
import static org.junit.jupiter.api.Assertions.*;

class ItemModelResolverTest {
    @Test
    void blockItemReferenceRetainsDeclaredModelPath() {
        var reference = ItemModel.reference(Key.key("the_sift:block/ichor_snow_height2"));
        assertSame(reference, ItemModelResolver.reference(reference));
        assertEquals("block/ichor_snow_height2", ItemModelResolver.reference(reference).model().value());
    }

    @Test
    void compositeSkipsEmptyChildren() {
        var reference = ItemModel.reference(Key.key("the_sift:item/siftite_ingot"));
        assertSame(reference, ItemModelResolver.reference(ItemModel.composite(ItemModel.empty(), reference)));
    }

    @Test
    void emptyAndMissingDefinitionsHaveNoStaticReference() {
        assertNull(ItemModelResolver.reference(null));
        assertNull(ItemModelResolver.reference(ItemModel.empty()));
        assertNull(ItemModelResolver.reference(ItemModel.composite(ItemModel.empty())));
    }
}

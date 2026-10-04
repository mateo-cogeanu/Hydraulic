package org.geysermc.hydraulic.item;

import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.item.*;

/** Resolves the static presentation of an item definition, including block model references. */
public final class ItemModelResolver {
    private ItemModelResolver() {
    }

    @Nullable
    public static ReferenceItemModel reference(@Nullable ItemModel model) {
        if (model instanceof ReferenceItemModel reference) {
            return reference;
        }
        if (model instanceof SelectItemModel select) {
            return reference(select.fallback());
        }
        if (model instanceof RangeDispatchItemModel range) {
            return reference(range.fallback());
        }
        if (model instanceof ConditionItemModel condition) {
            // The inactive presentation is the default for properties such as using_item.
            ReferenceItemModel reference = reference(condition.onFalse());
            return reference != null ? reference : reference(condition.onTrue());
        }
        if (model instanceof CompositeItemModel composite) {
            for (ItemModel child : composite.models()) {
                ReferenceItemModel reference = reference(child);
                if (reference != null) return reference;
            }
        }
        return null;
    }
}

package org.geysermc.hydraulic.block;

import team.unnamed.creative.blockstate.Condition;
import java.util.Map;

/** Java multipart conditions can nest AND/OR and list alternatives with '|'. */
public final class BlockStateConditions {
    private BlockStateConditions() {
    }

    public static boolean matches(Condition condition, Map<String, String> properties) {
        if (condition == Condition.NONE) return true;
        if (condition instanceof Condition.And and) {
            return and.conditions().stream().allMatch(child -> matches(child, properties));
        }
        if (condition instanceof Condition.Or or) {
            return or.conditions().stream().anyMatch(child -> matches(child, properties));
        }
        if (condition instanceof Condition.Match match) {
            String actual = properties.get(match.key());
            if (actual == null) return false;
            String expected = match.value().toString();
            boolean negate = expected.startsWith("!");
            if (negate) expected = expected.substring(1);
            boolean found = java.util.Arrays.asList(expected.split("\\|", -1)).contains(actual);
            return negate != found;
        }
        return false;
    }
}

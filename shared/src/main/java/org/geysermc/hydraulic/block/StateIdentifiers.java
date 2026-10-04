package org.geysermc.hydraulic.block;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Normalizes state names and projects mod-extended vanilla states onto known properties. */
public final class StateIdentifiers {
    private StateIdentifiers() {
    }

    public static String block(String identifier) {
        int bracket = identifier.indexOf('[');
        return bracket < 0 ? identifier : identifier.substring(0, bracket);
    }

    public static Map<String, String> properties(String identifier) {
        var values = new TreeMap<String, String>();
        int bracket = identifier.indexOf('[');
        if (bracket >= 0) {
            for (String property : identifier.substring(bracket + 1, identifier.length() - 1).split(",")) {
                String[] pair = property.split("=", 2);
                if (pair.length == 2) values.put(pair[0], pair[1]);
            }
        }
        return values;
    }

    public static String project(String identifier, Set<String> knownProperties) {
        Map<String, String> values = properties(identifier);
        values.keySet().retainAll(knownProperties);
        if (values.isEmpty()) return block(identifier);
        return block(identifier) + "[" + values.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(",")) + "]";
    }
}

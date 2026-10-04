package org.geysermc.hydraulic.block;

import java.util.*;

/** Exact set reduction: remove a property only when every value has identical components. */
public final class PermutationConditions {
    public static List<Map<String, String>> reduce(Collection<Map<String, String>> states, Map<String, Set<String>> domains) {
        Set<Map<String, String>> terms = new LinkedHashSet<>(); states.forEach(state -> terms.add(Map.copyOf(state)));
        boolean changed;
        do {
            changed = false;
            for (var property : new TreeMap<>(domains).entrySet()) {
                Map<Map<String, String>, Set<String>> buckets = new LinkedHashMap<>();
                for (var term : terms) {
                    if (!term.containsKey(property.getKey())) continue;
                    Map<String, String> rest = new HashMap<>(term); String value = rest.remove(property.getKey());
                    buckets.computeIfAbsent(Map.copyOf(rest), ignored -> new HashSet<>()).add(value);
                }
                for (var bucket : buckets.entrySet()) {
                    if (!bucket.getValue().equals(property.getValue())) continue;
                    terms.removeIf(term -> { var rest = new HashMap<>(term); return rest.remove(property.getKey()) != null && rest.equals(bucket.getKey()); });
                    terms.add(bucket.getKey()); changed = true;
                }
            }
        } while (changed);
        return List.copyOf(terms);
    }
    public static String condition(Collection<Map<String, String>> states, Map<String, Set<String>> domains) {
        return reduce(states, domains).stream().map(term -> term.isEmpty() ? "1.0" : "(" + new TreeMap<>(term).entrySet().stream()
                .map(entry -> "query.block_property('" + entry.getKey() + "') == " + entry.getValue())
                .collect(java.util.stream.Collectors.joining(" && ")) + ")").collect(java.util.stream.Collectors.joining(" || "));
    }
}

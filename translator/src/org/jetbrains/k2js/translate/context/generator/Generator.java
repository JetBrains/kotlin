package org.jetbrains.k2js.translate.context.generator;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author Pavel Talanov
 */
public final class Generator<K, V> {


    @NotNull
    private final Map<K, V> values = Maps.newHashMap();
    @NotNull
    private final List<Rule<K, V>> rules = Lists.newArrayList();

    public void addRule(@NotNull Rule<K, V> rule) {
        rules.add(rule);
    }

    @NotNull
    public V get(@NotNull K data) {
        V result = values.get(data);
        if (result != null) {
            return result;
        }
        return generate(data);
    }

    @NotNull
    private V generate(@NotNull K data) {
        V result = null;
        for (Rule<K, V> rule : rules) {
            result = rule.apply(data);
            if (result != null) {
                return result;
            }
        }
        throw new AssertionError("No rule applicable to generate result for " + data.toString());
    }
}

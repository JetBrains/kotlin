package org.jetbrains.k2js.translate.context.generator;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

import java.util.List;
import java.util.Map;

/**
 * @author Pavel Talanov
 */
public final class Generator<V> {


    @NotNull
    private final Map<DeclarationDescriptor, V> values = Maps.newHashMap();
    @NotNull
    private final List<Rule<V>> rules = Lists.newArrayList();

    public void addRule(@NotNull Rule<V> rule) {
        rules.add(rule);
    }

    @NotNull
    public V get(@NotNull DeclarationDescriptor descriptor) {
        V result = values.get(descriptor);
        if (result != null) {
            return result;
        }
        return generate(descriptor);
    }

    @NotNull
    private V generate(@NotNull DeclarationDescriptor descriptor) {
        V result = null;
        for (Rule<V> rule : rules) {
            result = rule.apply(descriptor);
            if (result != null) {
                return result;
            }
        }
        throw new AssertionError("No rule applicable to generate result for " + descriptor.toString());
    }
}

package org.jetbrains.k2js.translate.context.generator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

/**
 * @author Pavel Talanov
 */
public interface Rule<V> {

    @Nullable
    V apply(@NotNull DeclarationDescriptor descriptor);
}

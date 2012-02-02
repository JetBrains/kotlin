package org.jetbrains.k2js.translate.context.generator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Pavel Talanov
 */
public interface Rule<K, V> {

    @Nullable
    V apply(@NotNull K data);
}

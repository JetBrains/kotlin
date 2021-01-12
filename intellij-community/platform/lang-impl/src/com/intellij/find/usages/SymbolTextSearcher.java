// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.usages;

import com.intellij.model.Symbol;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Implement this interface and register as {@code com.intellij.lang.symbolTextSearcher} extension
 * to enable searching of text occurrences of the symbol.
 * <p/>
 * It's not necessary to implement this interface if there exist an implementation of {@link SymbolSearchTargetFactory} for some symbol,
 * since the returned {@link SearchTarget} should have its {@link SearchTarget#getTextSearchStrings} implemented.
 */
public interface SymbolTextSearcher<S extends Symbol> {

  @NotNull Collection<@NotNull String> getStringsToSearch(@NotNull S symbol);
}

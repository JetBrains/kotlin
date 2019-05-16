// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.usages;

import com.intellij.model.Symbol;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Implement this interface and register as {@code com.intellij.lang.symbolTextSearcher} extension
 * to enable searching of text occurrences of the symbol.
 * <p>
 * Text doesn't contain references by design (e.g. plain text or markdown),
 * but there might exist occurrences which are feasible to find/rename,
 * e.g fully qualified name of a Java class or package.
 * <p>
 * Returning non-empty collection will enable "Search for text occurrences" checkbox in the UI.
 */
public interface SymbolTextSearcher<S extends Symbol> {

  @NotNull Collection<@NotNull String> getStringsToSearch(@NotNull S symbol);
}

// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.usages;

import com.intellij.model.Symbol;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.jetbrains.annotations.ApiStatus.OverrideOnly;

/**
 * Implement this interface and register as {@code com.intellij.lang.symbolSearchTarget} extension
 * to customize search implementation and presentation at once.
 * <p/>
 * Several symbols might have {@link SearchTarget#equals equal} targets,
 * in this case any target will be chosen and used instead of showing the disambiguation popup.
 *
 * @see SymbolUsageHandlerFactory
 * @see SymbolTextSearcher
 */
@OverrideOnly
public interface SymbolSearchTargetFactory<T extends Symbol> {

  @Nullable
  SearchTarget createTarget(@NotNull Project project, @NotNull T symbol);
}

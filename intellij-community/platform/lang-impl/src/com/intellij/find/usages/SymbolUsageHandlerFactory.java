// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.usages;

import com.intellij.model.Symbol;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.jetbrains.annotations.ApiStatus.OverrideOnly;

/**
 * Implement this interface and register as {@code com.intellij.lang.symbolUsageHandler} extension
 * to customize search options and/or search query for the symbol.
 *
 * @see SymbolTextSearcher
 */
@OverrideOnly
public interface SymbolUsageHandlerFactory<T extends Symbol> {

  @Nullable
  UsageHandler<?> createHandler(@NotNull Project project, @NotNull T target);
}

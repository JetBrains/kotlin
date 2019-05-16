// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.usages;

import com.intellij.psi.search.SearchScope;
import com.intellij.usages.Usage;
import com.intellij.util.Query;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.jetbrains.annotations.Nls.Capitalization.Title;

/**
 * Base class for usage handlers without custom options.
 */
public abstract class NonConfigurableUsageHandler implements UsageHandler<@Nullable Void> {

  @Override
  public final @Nullable Void getCustomOptions(@NotNull UsageAction action) {
    return null;
  }

  @Override
  public final boolean hasAnythingToSearch(@Nullable Void customOptions) {
    return false;
  }

  @Override
  public final @Nls(capitalization = Title) @NotNull String getSearchString(@NotNull UsageOptions options, @Nullable Void customOptions) {
    return getSearchString(options);
  }

  @Override
  public final @NotNull Query<? extends @NotNull Usage> buildSearchQuery(@NotNull UsageOptions options, @Nullable Void customOptions) {
    return buildSearchQuery(options);
  }

  @Override
  public abstract @NotNull SearchScope getMaximalSearchScope();

  protected abstract @Nls(capitalization = Title) @NotNull String getSearchString(@NotNull UsageOptions options);

  protected abstract @NotNull Query<? extends @NotNull Usage> buildSearchQuery(@NotNull UsageOptions options);
}

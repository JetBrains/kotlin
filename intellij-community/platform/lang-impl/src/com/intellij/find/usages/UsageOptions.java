// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.usages;

import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface UsageOptions {

  /**
   * @return whether usage search is needed, e.g. _Usages_ checkbox is selected
   */
  boolean isUsages();

  /**
   * @return search scope selected by the user or by the platform (e.g. when highlighting identifiers)
   */
  @NotNull SearchScope getSearchScope();

  @Contract(value = "_ -> new", pure = true)
  static @NotNull UsageOptions createOptions(@NotNull SearchScope searchScope) {
    return createOptions(true, searchScope);
  }

  @Contract(value = "_, _ -> new", pure = true)
  static @NotNull UsageOptions createOptions(boolean usages, @NotNull SearchScope searchScope) {
    return new UsageOptions() {

      @Override
      public boolean isUsages() {
        return usages;
      }

      @Override
      public @NotNull SearchScope getSearchScope() {
        return searchScope;
      }
    };
  }
}

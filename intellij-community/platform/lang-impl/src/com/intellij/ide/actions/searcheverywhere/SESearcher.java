// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

interface SESearcher {
  ProgressIndicator search(@NotNull Map<? extends SearchEverywhereContributor<?>, Integer> contributorsAndLimits,
                           @NotNull String pattern);

  ProgressIndicator findMoreItems(@NotNull Map<? extends SearchEverywhereContributor<?>, Collection<SearchEverywhereFoundElementInfo>> alreadyFound,
                                  @NotNull String pattern,
                                  @NotNull SearchEverywhereContributor<?> contributor,
                                  int newLimit);

  /**
   * Search process listener interface
   */
  interface Listener {
    void elementsAdded(@NotNull List<? extends SearchEverywhereFoundElementInfo> list);
    void elementsRemoved(@NotNull List<? extends SearchEverywhereFoundElementInfo> list);
    void searchFinished(@NotNull Map<SearchEverywhereContributor<?>, Boolean> hasMoreContributors);
  }
}

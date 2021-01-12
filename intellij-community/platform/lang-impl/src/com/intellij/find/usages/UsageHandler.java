// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.usages;

import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.usages.Usage;
import com.intellij.util.Query;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import static org.jetbrains.annotations.Nls.Capitalization.Title;

/**
 * @param <O> type of search options.
 *            Nullability is omitted intentionally because implementations are responsible for it,
 *            e.g. if {@link #getCustomOptions} returns {@code null},
 *            then the same {@code null} is passed into {@link #buildSearchQuery} and {@link #getSearchString}.
 * @see NonConfigurableUsageHandler
 * @see SymbolUsageHandlerFactory
 */
public interface UsageHandler<O> {

  /**
   * <ul>
   *   <li>Returning {@link LocalSearchScope} will also make search scope unavailable to change in the UI.</li>
   *   <li>Maximal scope is used to rerun Show Usages if user scope differs from maximal scope.</li>
   * </ul>
   *
   * @return maximal search scope where this usage handler might yield any results
   */
  @NotNull SearchScope getMaximalSearchScope();

  enum UsageAction {
    FIND_USAGES,
    SHOW_USAGES,
    ;
  }

  /**
   * Returned instance may be used to {@link #buildSearchQuery build search query},
   * or to initialize UI to obtain another instance of custom options configured by the user.
   * Custom options must have associated {@link com.intellij.openapi.options.OptionEditorProvider option editor provider}.
   *
   * @return instance of custom options or {@code null} if the handler doesn't have additional configuration
   */
  O getCustomOptions(@NotNull UsageAction action);

  /**
   * @return whether the combination of selected option might yield any results;
   * returning {@code false} from this method may prevent even starting the search
   */
  boolean hasAnythingToSearch(O customOptions);

  /**
   * @return search string to be shown in the Usage View and/or Usage Popup,
   * e.g. <i>Usages and Implementations of Method 'foo' of Class 'X'</i>
   */
  @Nls(capitalization = Title) @NotNull String getSearchString(@NotNull UsageOptions options, O customOptions);

  /**
   * @return query which will be executed on the background thread later
   */
  @NotNull Query<? extends @NotNull Usage> buildSearchQuery(@NotNull UsageOptions options, O customOptions);
}

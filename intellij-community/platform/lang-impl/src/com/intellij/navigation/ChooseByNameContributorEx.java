// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.navigation;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public interface ChooseByNameContributorEx extends ChooseByNameContributor {

  void processNames(@NotNull Processor<String> processor,
                    @NotNull GlobalSearchScope scope,
                    @Nullable IdFilter filter);

  void processElementsWithName(@NotNull String name,
                               @NotNull Processor<NavigationItem> processor,
                               @NotNull FindSymbolParameters parameters);

  /** @deprecated Use {@link #processNames(Processor, GlobalSearchScope, IdFilter)} instead */
  @Deprecated
  @Override
  @NotNull
  default String[] getNames(Project project, boolean includeNonProjectItems) {
    List<String> result = new ArrayList<>();
    processNames(result::add, FindSymbolParameters.searchScopeFor(project, includeNonProjectItems), null);
    return ArrayUtilRt.toStringArray(result);
  }

  /** @deprecated Use {@link #processElementsWithName(String, Processor, FindSymbolParameters)} instead */
  @Deprecated
  @Override
  @NotNull
  default NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
    List<NavigationItem> result = new ArrayList<>();
    processElementsWithName(name, result::add, FindSymbolParameters.simple(project, includeNonProjectItems));
    return result.isEmpty() ? NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY : result.toArray(NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY);
  }
}

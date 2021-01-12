// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractEqualityProvider implements SEResultsEqualityProvider {

  @Override
  public @NotNull SEEqualElementsActionType compareItems(@NotNull SearchEverywhereFoundElementInfo newItem,
                                                         @NotNull SearchEverywhereFoundElementInfo alreadyFoundItem) {
    if (areEqual(newItem, alreadyFoundItem)) {
      return SearchEverywhereFoundElementInfo.COMPARATOR.compare(newItem, alreadyFoundItem) > 0
             ? SEEqualElementsActionType.REPLACE
             : SEEqualElementsActionType.SKIP;
    }

    return SEEqualElementsActionType.DO_NOTHING;
  }

  protected abstract boolean areEqual(@NotNull SearchEverywhereFoundElementInfo newItem,
                                      @NotNull SearchEverywhereFoundElementInfo alreadyFoundItem);
}

// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TrivialElementsEqualityProvider implements SEResultsEqualityProvider {

  @NotNull
  @Override
  public SEEqualElementsActionType compareItems(@NotNull SearchEverywhereFoundElementInfo newItem, @NotNull SearchEverywhereFoundElementInfo alreadyFoundItem) {
    if (Objects.equals(newItem.getElement(), alreadyFoundItem.getElement())) {
      return newItem.getPriority() > alreadyFoundItem.getPriority() ? SEEqualElementsActionType.REPLACE : SEEqualElementsActionType.SKIP;
    }
    return SEEqualElementsActionType.DO_NOTHING;
  }
}

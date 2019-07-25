// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public interface SEResultsEqualityProvider {

  ExtensionPointName<SEResultsEqualityProvider> EP_NAME = ExtensionPointName.create("com.intellij.searchEverywhereResultsEqualityProvider");

  enum SEEqualElementsActionType {
    DO_NOTHING, SKIP, REPLACE
  }

  @NotNull
  SEEqualElementsActionType compareItems(@NotNull SearchEverywhereFoundElementInfo newItem, @NotNull SearchEverywhereFoundElementInfo alreadyFoundItem);

  @NotNull
  static List<SEResultsEqualityProvider> getProviders() {
    return Arrays.asList(EP_NAME.getExtensions());
  }

  @NotNull
  static SEResultsEqualityProvider composite(@NotNull Collection<? extends SEResultsEqualityProvider> providers) {
    return new SEResultsEqualityProvider() {
      @NotNull
      @Override
      public SEEqualElementsActionType compareItems(@NotNull SearchEverywhereFoundElementInfo newItem, @NotNull SearchEverywhereFoundElementInfo alreadyFoundItem) {
        return providers.stream()
          .map(provider -> provider.compareItems(newItem, alreadyFoundItem))
          .filter(action -> action != SEEqualElementsActionType.DO_NOTHING)
          .findFirst()
          .orElse(SEEqualElementsActionType.DO_NOTHING);
      }
    };
  }
}

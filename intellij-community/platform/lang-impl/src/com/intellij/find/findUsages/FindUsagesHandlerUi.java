// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.findUsages;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FindUsagesHandlerUi {
  @NotNull
  AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab);


  @Nullable
  default String getHelpId() {
    return null;
  }
}

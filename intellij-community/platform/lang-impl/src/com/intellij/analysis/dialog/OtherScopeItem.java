/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.analysis.dialog;

import com.intellij.analysis.AnalysisScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OtherScopeItem implements ModelScopeItem {
  private final AnalysisScope myScope;

  @Nullable
  public static OtherScopeItem tryCreate(@NotNull AnalysisScope scope) {
    if (scope.getScopeType() != AnalysisScope.PROJECT
        && scope.getScopeType() != AnalysisScope.MODULE
        && scope.getScopeType() != AnalysisScope.UNCOMMITTED_FILES
        && scope.getScopeType() != AnalysisScope.CUSTOM) {
      return new OtherScopeItem(scope);
    }
    return null;
  }

  public OtherScopeItem(AnalysisScope scope) {
    myScope = scope;
  }

  @Override
  public AnalysisScope getScope() {
    return myScope;
  }
}
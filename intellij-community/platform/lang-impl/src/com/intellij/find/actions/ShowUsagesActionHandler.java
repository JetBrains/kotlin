// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.actions;

import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.psi.search.SearchScope;
import com.intellij.usages.UsageSearchPresentation;
import com.intellij.usages.UsageSearcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

interface ShowUsagesActionHandler {

  boolean isValid();

  @NotNull UsageSearchPresentation getPresentation();

  @NotNull UsageSearcher createUsageSearcher();

  void findUsages();

  @Nullable ShowUsagesActionHandler showDialog();

  @NotNull ShowUsagesActionHandler withScope(@NotNull SearchScope searchScope);

  @NotNull SearchScope getSelectedScope();

  @NotNull SearchScope getMaximalScope();

  static @Nullable String getSecondInvocationTitle(@NotNull ShowUsagesActionHandler actionHandler) {
    KeyboardShortcut shortcut = ShowUsagesAction.getShowUsagesShortcut();
    if (shortcut == null) {
      return null;
    }
    SearchScope maximalScope = actionHandler.getMaximalScope();
    if (actionHandler.getSelectedScope().equals(maximalScope)) {
      return null;
    }
    return "Press " + KeymapUtil.getShortcutText(shortcut) + " again to search in " + maximalScope.getDisplayName();
  }
}

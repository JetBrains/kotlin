// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.bookmarks.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import org.jetbrains.annotations.NotNull;

class ToggleSortBookmarksAction extends ToggleAction {
  ToggleSortBookmarksAction() {
    super(null, IdeBundle.message("action.bookmark.toggle.sort"), AllIcons.ObjectBrowser.Sorted);
    setEnabledInModalContext(true);
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return UISettings.getInstance().getSortBookmarks();
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    UISettings.getInstance().setSortBookmarks(state);
    UISettings.getInstance().fireUISettingsChanged();
  }
}
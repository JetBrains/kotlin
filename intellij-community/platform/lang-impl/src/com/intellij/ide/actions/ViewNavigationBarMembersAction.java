// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

public class ViewNavigationBarMembersAction extends ToggleAction implements DumbAware {
  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return UISettings.getInstance().getShowMembersInNavigationBar();
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    UISettings uiSettings = UISettings.getInstance();
    uiSettings.setShowMembersInNavigationBar(state);
    uiSettings.fireUISettingsChanged();
    EditorSettingsExternalizable.getInstance().resetDefaultBreadcrumbVisibility();
  }
}

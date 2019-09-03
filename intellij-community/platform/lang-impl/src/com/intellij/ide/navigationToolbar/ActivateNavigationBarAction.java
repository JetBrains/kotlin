// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.navigationToolbar;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.IdeRootPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Anna Kozlova
 * @author Konstantin Bulenkov
 */
class ActivateNavigationBarAction extends AnAction implements DumbAware {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project != null && UISettings.getInstance().getShowNavigationBar()) {
      JFrame frame = WindowManagerEx.getInstanceEx().getFrame(project);
      IdeRootPane ideRootPane = (IdeRootPane)frame.getRootPane();
      JComponent component = ideRootPane.findByName(NavBarRootPaneExtension.NAV_BAR).getComponent();
      if (component instanceof NavBarPanel) {
        final NavBarPanel navBarPanel = (NavBarPanel)component;
        navBarPanel.rebuildAndSelectTail(true);
      }
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    UISettings settings = UISettings.getInstance();
    final boolean enabled = project != null && settings.getShowNavigationBar() && !settings.getPresentationMode();
    e.getPresentation().setEnabled(enabled);
  }
}

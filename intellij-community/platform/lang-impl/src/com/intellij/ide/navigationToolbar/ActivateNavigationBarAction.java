/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.navigationToolbar;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.openapi.wm.impl.IdeRootPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Anna Kozlova
 * @author Konstantin Bulenkov
 */
public class ActivateNavigationBarAction extends AnAction implements DumbAware {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project != null && UISettings.getInstance().getShowNavigationBar()) {
      final IdeFrameImpl frame = WindowManagerEx.getInstanceEx().getFrame(project);
      final IdeRootPane ideRootPane = (IdeRootPane)frame.getRootPane();
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

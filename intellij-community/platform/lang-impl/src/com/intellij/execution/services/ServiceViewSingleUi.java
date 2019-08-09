// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

class ServiceViewSingleUi implements ServiceViewUi {
  private final SimpleToolWindowPanel myMainPanel = new SimpleToolWindowPanel(false);
  private final JPanel myMessagePanel = new JBPanelWithEmptyText().withEmptyText("No content available");

  ServiceViewSingleUi() {
    UIUtil.putClientProperty(myMainPanel, UIUtil.NOT_IN_HIERARCHY_COMPONENTS,
                             (Iterable<JComponent>)() -> JBIterable.of((JComponent)myMessagePanel)
                               .filter(component -> myMainPanel != component.getParent()).iterator());
    myMessagePanel.setFocusable(true);
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myMainPanel;
  }

  @Override
  public void saveState(@NotNull ServiceViewState state) {
  }

  @Override
  public void setServiceToolbar(@NotNull ServiceViewActionProvider actionManager) {
    ActionToolbar toolbar = actionManager.createServiceToolbar(myMainPanel);
    myMainPanel.setToolbar(actionManager.wrapServiceToolbar(toolbar));
  }

  @Override
  public void setMasterComponent(@NotNull JComponent component, @NotNull ServiceViewActionProvider actionManager) {
  }

  @Override
  public void setDetailsComponent(@Nullable JComponent component) {
    if (component == null) {
      component = myMessagePanel;
    }
    if (component.getParent() == myMainPanel) return;

    myMainPanel.setContent(component);
  }

  @Override
  public void setNavBar(@NotNull JComponent component) {
  }

  @Override
  public void setMasterComponentVisible(boolean visible) {
  }

  @Nullable
  @Override
  public JComponent getDetailsComponent() {
    JComponent content = myMainPanel.getContent();
    return content == myMessagePanel ? null : content;
  }
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBPanelWithEmptyText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ServiceViewSingleUi implements ServiceViewUi {
  private final SimpleToolWindowPanel myMainPanel = new SimpleToolWindowPanel(false);
  private final JPanel myMessagePanel = new JBPanelWithEmptyText().withEmptyText("No content available");

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
    myMainPanel.setToolbar(actionManager.createServiceToolbar(myMainPanel));
  }

  @Override
  public void setMasterPanel(@NotNull JComponent component, @NotNull ServiceViewActionProvider actionManager) {
  }

  @Override
  public void setDetailsComponent(@Nullable JComponent component) {
    if (component == null) {
      component = myMessagePanel;
    }
    if (component.getParent() == myMainPanel) return;

    myMainPanel.setContent(component);
  }
}

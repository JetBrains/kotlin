// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.execution.services.ServiceViewDragHelper.ServiceViewDragBean;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class ExtractServiceAction extends DumbAwareAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    List<ServiceViewItem> items = getSelectedItems(e);
    e.getPresentation().setEnabled(!items.isEmpty());
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    List<ServiceViewItem> items = getSelectedItems(e);
    if (items.isEmpty()) return;

    ((ServiceViewManagerImpl)ServiceViewManager.getInstance(e.getProject())).extract(new ServiceViewDragBean(items));
  }

  private static List<ServiceViewItem> getSelectedItems(@NotNull AnActionEvent e) {
    Component contextComponent = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    while (contextComponent != null && !(contextComponent instanceof ServiceView)) {
      contextComponent = contextComponent.getParent();
    }
    return contextComponent == null ? Collections.emptyList() : ((ServiceView)contextComponent).getSelectedItems();
  }
}

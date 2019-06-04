// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.ide.DeleteProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

class ServiceViewDeleteProvider implements DeleteProvider {
  private final ServiceView myServiceView;

  ServiceViewDeleteProvider(@NotNull ServiceView serviceView) {
    myServiceView = serviceView;
  }

  @Override
  public void deleteElement(@NotNull DataContext dataContext) {
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) return;

    List<Pair<ServiceViewItem, Runnable>> items = ContainerUtil.mapNotNull(myServiceView.getSelectedItems(), item -> {
      Runnable remover = item.getViewDescriptor().getRemover();
      return remover == null ? null : Pair.create(item, remover);
    });
    items = filterChildren(items);
    if (items.isEmpty()) return;

    int size = items.size();
    if (Messages.showYesNoDialog(project,
                                 "Delete " + size + " " + StringUtil.pluralize("item", size) + "?",
                                 "Delete",
                                 Messages.getWarningIcon())
        != Messages.YES) {
      return;
    }
    for (Pair<ServiceViewItem, Runnable> item : items) {
      item.second.run();
    }
  }

  @Override
  public boolean canDeleteElement(@NotNull DataContext dataContext) {
    if (myServiceView.getSelectedItems().stream().noneMatch(item -> item.getViewDescriptor().getRemover() != null)) {
      return false;
    }
    JComponent detailsComponent = myServiceView.getUi().getDetailsComponent();
    return detailsComponent == null || !UIUtil.isAncestor(detailsComponent, dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT));
  }

  @NotNull
  private static List<Pair<ServiceViewItem, Runnable>> filterChildren(List<Pair<ServiceViewItem, Runnable>> items) {
    return ContainerUtil.filter(items, item -> {
      ServiceViewItem parent = item.first.getParent();
      while (parent != null) {
        for (Pair<ServiceViewItem, Runnable> pair : items) {
          if (pair.first.equals(parent)) {
            return false;
          }
        }
        parent = parent.getParent();
      }
      return true;
    });
  }
}

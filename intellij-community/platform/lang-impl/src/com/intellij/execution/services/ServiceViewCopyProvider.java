// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.ide.CopyProvider;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;

class ServiceViewCopyProvider implements CopyProvider {
  private final ServiceView myServiceView;

  ServiceViewCopyProvider(@NotNull ServiceView serviceView) {
    myServiceView = serviceView;
  }

  @Override
  public void performCopy(@NotNull DataContext dataContext) {
    List<ServiceViewItem> items = myServiceView.getSelectedItems();
    if (!items.isEmpty()) {
      CopyPasteManager.getInstance().setContents(new StringSelection(
        StringUtil.join(items, item -> ServiceViewDragHelper.getDisplayName(item.getViewDescriptor().getPresentation()), "\n")));
    }
  }

  @Override
  public boolean isCopyEnabled(@NotNull DataContext dataContext) {
    if (myServiceView.getSelectedItems().isEmpty()) {
      return false;
    }
    JComponent detailsComponent = myServiceView.getUi().getDetailsComponent();
    return detailsComponent == null || !UIUtil.isAncestor(detailsComponent, dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT));
  }

  @Override
  public boolean isCopyVisible(@NotNull DataContext dataContext) {
    return false;
  }
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.execution.services.ServiceViewNavBarPanel.ServiceViewNavBarRoot;
import com.intellij.icons.AllIcons;
import com.intellij.ide.navigationToolbar.AbstractNavBarModelExtension;
import com.intellij.openapi.actionSystem.DataProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ServiceViewNavBarExtension extends AbstractNavBarModelExtension {
  @Nullable
  @Override
  public String getPopupMenuGroup(@NotNull DataProvider provider) {
    ServiceView serviceView = ServiceViewActionProvider.getSelectedView(provider);
    return serviceView == null ? null : ServiceViewActionProvider.SERVICE_VIEW_ITEM_POPUP;
  }

  @Nullable
  @Override
  public String getPresentableText(Object object) {
    if (object instanceof ServiceViewItem) {
      return ServiceViewDragHelper.getDisplayName(((ServiceViewItem)object).getViewDescriptor().getPresentation());
    }
    if (object instanceof ServiceViewNavBarRoot) {
      return "";
    }
    return null;
  }

  @Nullable
  @Override
  public Icon getIcon(Object object) {
    if (object instanceof ServiceViewItem) {
      return ((ServiceViewItem)object).getViewDescriptor().getPresentation().getIcon(false);
    }
    if (object instanceof ServiceViewNavBarRoot) {
      return AllIcons.Nodes.Services;
    }
    return null;
  }
}

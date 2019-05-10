// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import java.awt.*;
import java.util.List;

abstract class ServiceView extends JPanel implements Disposable {
  protected final Project myProject;
  private final ServiceViewModel myModel;
  protected final ServiceViewUi myUi;

  ServiceView(LayoutManager layout, @NotNull Project project, @NotNull ServiceViewModel model, @NotNull ServiceViewUi ui) {
    super(layout);
    myProject = project;
    myModel = model;
    myUi = ui;
  }

  @Override
  public void dispose() {
  }

  ServiceViewModel getModel() {
    return myModel;
  }

  abstract void saveState(@NotNull ServiceViewState state);

  @NotNull
  abstract List<ServiceViewItem> getSelectedItems();

  abstract Promise<Void> select(@NotNull Object service, @NotNull Class<?> contributorClass);

  abstract void onViewSelected();

  abstract void onViewUnselected();

  static ServiceView createTreeView(@NotNull Project project, @NotNull ServiceViewModel model, @NotNull ServiceViewState state) {
    ServiceViewUi ui = new ServiceViewTreeUi(state);
    ServiceView serviceView = new ServiceTreeView(project, model, ui, state);
    setDataProvider(serviceView);
    return serviceView;
  }

  static ServiceView createSingleView(@NotNull Project project, @NotNull ServiceViewModel model) {
    ServiceViewUi ui = new ServiceViewSingleUi();
    ServiceView serviceView = new ServiceSingleView(project, model, ui);
    setDataProvider(serviceView);
    return serviceView;
  }

  private static void setDataProvider(ServiceView serviceView) {
    serviceView.putClientProperty(DataManager.CLIENT_PROPERTY_DATA_PROVIDER, (DataProvider)dataId -> {
      if (PlatformDataKeys.HELP_ID.is(dataId)) {
        return ServiceViewManagerImpl.getToolWindowContextHelpId();
      }
      if (PlatformDataKeys.SELECTED_ITEMS.is(dataId)) {
        return ContainerUtil.map2Array(serviceView.getSelectedItems(), ServiceViewItem::getValue);
      }
      List<ServiceViewItem> selectedItems = serviceView.getSelectedItems();
      ServiceViewItem selectedItem = ContainerUtil.getOnlyItem(selectedItems);
      ServiceViewDescriptor descriptor = selectedItem == null ? null : selectedItem.getViewDescriptor();
      DataProvider dataProvider = descriptor == null ? null : descriptor.getDataProvider();
      if (dataProvider != null) {
        return RecursionManager.doPreventingRecursion(serviceView, false, () -> dataProvider.getData(dataId));
      }
      return null;
    });
  }
}

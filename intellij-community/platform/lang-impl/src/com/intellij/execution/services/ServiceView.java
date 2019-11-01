// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.ide.DataManager;
import com.intellij.ide.DeleteProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.pom.Navigatable;
import com.intellij.ui.AutoScrollToSourceHandler;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

abstract class ServiceView extends JPanel implements Disposable {
  private final Project myProject;
  private final ServiceViewModel myModel;
  protected final ServiceViewUi myUi;
  private AutoScrollToSourceHandler myAutoScrollToSourceHandler;

  protected ServiceView(LayoutManager layout, @NotNull Project project, @NotNull ServiceViewModel model, @NotNull ServiceViewUi ui) {
    super(layout);
    myProject = project;
    myModel = model;
    myUi = ui;
  }

  @Override
  public void dispose() {
  }

  Project getProject() {
    return myProject;
  }

  ServiceViewModel getModel() {
    return myModel;
  }

  ServiceViewUi getUi() {
    return myUi;
  }

  void saveState(@NotNull ServiceViewState state) {
    myModel.saveState(state);
  }

  @NotNull
  abstract List<ServiceViewItem> getSelectedItems();

  abstract Promise<Void> select(@NotNull Object service, @NotNull Class<?> contributorClass);

  abstract void onViewSelected();

  abstract void onViewUnselected();

  boolean isGroupByServiceGroups() {
    return myModel.isGroupByServiceGroups();
  }

  void setGroupByServiceGroups(boolean value) {
    myModel.setGroupByServiceGroups(value);
  }

  boolean isGroupByContributor() {
    return myModel.isGroupByContributor();
  }

  void setGroupByContributor(boolean value) {
    myModel.setGroupByContributor(value);
  }

  abstract List<Object> getChildrenSafe(@NotNull List<Object> valueSubPath);

  void setAutoScrollToSourceHandler(@NotNull AutoScrollToSourceHandler autoScrollToSourceHandler) {
    myAutoScrollToSourceHandler = autoScrollToSourceHandler;
  }

  protected void onViewSelected(@NotNull ServiceViewDescriptor descriptor) {
    descriptor.onNodeSelected();
    if (myAutoScrollToSourceHandler != null) {
      myAutoScrollToSourceHandler.onMouseClicked(this);
    }
  }

  abstract void jumpToServices();

  abstract boolean hasItems();

  static ServiceView createView(@NotNull Project project, @NotNull ServiceViewModel viewModel, @NotNull ServiceViewState viewState) {
    setViewModelState(viewModel, viewState);
    ServiceView serviceView = viewModel instanceof ServiceViewModel.SingeServiceModel ?
                              createSingleView(project, viewModel) :
                              createTreeView(project, viewModel, viewState);
    setDataProvider(serviceView);
    return serviceView;
  }

  private static ServiceView createTreeView(@NotNull Project project, @NotNull ServiceViewModel model, @NotNull ServiceViewState state) {
    return new ServiceTreeView(project, model, new ServiceViewTreeUi(state), state);
  }

  private static ServiceView createSingleView(@NotNull Project project, @NotNull ServiceViewModel model) {
    return new ServiceSingleView(project, model, new ServiceViewSingleUi());
  }

  private static void setDataProvider(ServiceView serviceView) {
    ServiceViewOptions viewOptions = new ServiceViewOptions() {
      @Override
      public boolean isGroupByContributor() {
        return serviceView.isGroupByContributor();
      }

      @Override
      public boolean isGroupByServiceGroups() {
        return serviceView.isGroupByServiceGroups();
      }
    };
    serviceView.putClientProperty(DataManager.CLIENT_PROPERTY_DATA_PROVIDER, (DataProvider)dataId -> {
      if (PlatformDataKeys.HELP_ID.is(dataId)) {
        return ServiceViewManagerImpl.getToolWindowContextHelpId();
      }
      if (PlatformDataKeys.SELECTED_ITEMS.is(dataId)) {
        return ContainerUtil.map2Array(serviceView.getSelectedItems(), ServiceViewItem::getValue);
      }
      if (CommonDataKeys.NAVIGATABLE_ARRAY.is(dataId)) {
        List<Navigatable> navigatables =
          ContainerUtil.mapNotNull(serviceView.getSelectedItems(), item -> item.getViewDescriptor().getNavigatable());
        return navigatables.toArray(new Navigatable[0]);
      }
      if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER.is(dataId)) {
        List<ServiceViewItem> selection = serviceView.getSelectedItems();
        ServiceViewContributor contributor = ServiceViewDragHelper.getTheOnlyRootContributor(selection);
        DataProvider delegate = contributor == null ? null : contributor.getViewDescriptor(serviceView.getProject()).getDataProvider();
        DeleteProvider deleteProvider = delegate == null ? null : PlatformDataKeys.DELETE_ELEMENT_PROVIDER.getData(delegate);
        if (deleteProvider == null) return new ServiceViewDeleteProvider(serviceView);

        if (deleteProvider instanceof ServiceViewContributorDeleteProvider) {
          ((ServiceViewContributorDeleteProvider)deleteProvider).setFallbackProvider(new ServiceViewDeleteProvider(serviceView));
        }
        return deleteProvider;
      }
      if (PlatformDataKeys.COPY_PROVIDER.is(dataId)) {
        return new ServiceViewCopyProvider(serviceView);
      }
      if (ServiceViewActionUtils.CONTRIBUTORS_KEY.is(dataId)) {
        return serviceView.getModel().getRoots().stream().map(item -> item.getRootContributor()).collect(Collectors.toSet());
      }
      if (ServiceViewActionUtils.OPTIONS_KEY.is(dataId)) {
        return viewOptions;
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

  private static void setViewModelState(@NotNull ServiceViewModel viewModel, @NotNull ServiceViewState viewState) {
    viewModel.setGroupByServiceGroups(viewState.groupByServiceGroups);
    viewModel.setGroupByContributor(viewState.groupByContributor);
  }
}

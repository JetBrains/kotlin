// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.execution.services.ServiceViewModel.ServiceViewModelListener;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AppUIUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class ServiceSingleView extends ServiceView {
  private final AtomicReference<ServiceViewItem> myRef = new AtomicReference<>();
  private boolean mySelected;
  private final ServiceViewModelListener myListener;

  ServiceSingleView(@NotNull Project project, @NotNull ServiceViewModel model, @NotNull ServiceViewUi ui) {
    super(new BorderLayout(), project, model, ui);
    ui.setServiceToolbar(ServiceViewActionProvider.getInstance());
    add(ui.getComponent(), BorderLayout.CENTER);
    myListener = new ServiceViewModelListener() {
      @Override
      public void rootsChanged() {
        updateItem();
      }
    };
    model.addModelListener(myListener);
    model.getInvoker().invokeLater(this::updateItem);
  }

  @NotNull
  @Override
  Promise<Void> select(@NotNull Object service,
                       @NotNull Class<?> contributorClass) {
    ServiceViewItem item = myRef.get();
    if (item == null || !item.getValue().equals(service)) {
      return Promises.rejectedPromise("Service not found");
    }

    showContent();
    return Promises.resolvedPromise();
  }

  @Override
  void onViewSelected() {
    showContent();
  }

  @Override
  void onViewUnselected() {
    mySelected = false;
    ServiceViewItem item = myRef.get();
    if (item != null) {
      item.getViewDescriptor().onNodeUnselected();
    }
  }

  @NotNull
  @Override
  List<ServiceViewItem> getSelectedItems() {
    ServiceViewItem item = myRef.get();
    return item == null ? Collections.emptyList() : Collections.singletonList(item);
  }

  @Override
  public void dispose() {
    getModel().removeModelListener(myListener);
  }

  private void updateItem() {
    ServiceViewItem oldValue = myRef.get();
    ServiceViewItem newValue = ContainerUtil.getOnlyItem(getModel().getRoots());
    myRef.set(newValue);
    AppUIUtil.invokeOnEdt(() -> {
      if (mySelected) {
        if (newValue != null) {
          ServiceViewDescriptor descriptor = newValue.getViewDescriptor();
          if (oldValue == null) {
            onViewSelected(descriptor);
          }
          myUi.setDetailsComponent(descriptor.getContentComponent());
        }
      }
    }, myProject.getDisposed());
  }

  private void showContent() {
    if (mySelected) return;

    mySelected = true;
    ServiceViewItem item = myRef.get();
    if (item != null) {
      ServiceViewDescriptor descriptor = item.getViewDescriptor();
      onViewSelected(descriptor);

      myUi.setDetailsComponent(descriptor.getContentComponent());
    }
  }

  @Override
  List<Object> getChildrenSafe(@NotNull Object value) {
    return Collections.emptyList();
  }
}

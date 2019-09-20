// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.services.ServiceModel.ServiceViewItem;
import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class ServiceModelFilter {
  private final List<ServiceViewFilter> myFilters = new CopyOnWriteArrayList<>();

  void addFilter(@NotNull ServiceViewFilter filter) {
    myFilters.add(filter);
  }

  void removeFilter(@NotNull ServiceViewFilter filter) {
    ServiceViewFilter parent = filter.getParent();
    myFilters.remove(filter);
    for (ServiceViewFilter viewFilter : myFilters) {
      if (viewFilter.getParent() == filter) {
        viewFilter.setParent(parent);
      }
    }
  }

  @NotNull
  List<? extends ServiceViewItem> filter(@NotNull List<? extends ServiceViewItem> items, @NotNull ServiceViewFilter targetFilter) {
    if (items.isEmpty()) return items;

    List<ServiceViewFilter> filters = excludeTargetAndParents(targetFilter);
    return ContainerUtil.filter(items, item -> filters.stream().noneMatch(filter -> filter.value(item)));
  }

  private List<ServiceViewFilter> excludeTargetAndParents(@NotNull ServiceViewFilter targetFilter) {
    List<ServiceViewFilter> filters = new ArrayList<>(myFilters);
    do {
      filters.remove(targetFilter);
      targetFilter = targetFilter.getParent();
    }
    while (targetFilter != null);
    return filters;
  }

  abstract static class ServiceViewFilter implements Condition<ServiceViewItem> {
    private ServiceViewFilter myParent;

    protected ServiceViewFilter(@Nullable ServiceViewFilter parent) {
      myParent = parent;
    }

    @Nullable
    ServiceViewFilter getParent() {
      return myParent;
    }

    private void setParent(@Nullable ServiceViewFilter parent) {
      myParent = parent;
    }
  }
}

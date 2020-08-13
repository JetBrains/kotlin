// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.projectView;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.scopeView.ScopeViewPane;
import com.intellij.internal.statistic.beans.MetricEvent;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

import static com.intellij.internal.statistic.beans.MetricEventFactoryKt.newMetric;

public final class ProjectViewCollector extends ProjectUsagesCollector {
  @Override
  public @NonNls @NotNull String getGroupId() {
    return "project.view.pane";
  }

  @Override
  protected @NotNull Set<MetricEvent> getMetrics(final @NotNull Project project) {
    final ProjectView projectView = ProjectView.getInstance(project);
    if (projectView == null) {
      return Collections.emptySet();
    }

    final AbstractProjectViewPane currentViewPane = projectView.getCurrentProjectViewPane();
    if (currentViewPane == null) {
      return Collections.emptySet();
    }

    final FeatureUsageData data = new FeatureUsageData()
      .addPluginInfo(PluginInfoDetectorKt.getPluginInfo(currentViewPane.getClass()))
      .addData("class_name", currentViewPane.getClass().getName());
    final NamedScope selectedScope = currentViewPane instanceof ScopeViewPane ? ((ScopeViewPane)currentViewPane).getSelectedScope() : null;
    if (selectedScope != null) {
      data.addData("scope_class_name", selectedScope.getClass().getName());
    }

    return Collections.singleton(newMetric("current", data));
  }
}

// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.internal.statistic.utils.StatisticsUtilKt;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Konstantin Aleev
 */
public class RunDashboardUsagesCollector extends ProjectUsagesCollector {
  @NotNull
  @Override
  public String getGroupId() {
    return "run.dashboard";
  }

  @NotNull
  @Override
  public Set<UsageDescriptor> getUsages(@NotNull Project project) {
    final Set<UsageDescriptor> usages = new HashSet<>();
    final Set<String> dashboardTypes = RunDashboardManager.getInstance(project).getTypes();
    usages.add(StatisticsUtilKt.getBooleanUsage("run.dashboard", !dashboardTypes.isEmpty()));
    if (!dashboardTypes.isEmpty()) {
      List<ConfigurationType> configurationTypes = ConfigurationType.CONFIGURATION_TYPE_EP.getExtensionList();
      for (String dashboardType : dashboardTypes) {
        ConfigurationType configurationType = ContainerUtil.find(configurationTypes, type -> type.getId().equals(dashboardType));
        if (configurationType == null) continue;

        String key = PluginInfoDetectorKt.getPluginInfo(configurationType.getClass()).isDevelopedByJetBrains() ?
                     dashboardType : "third.party";
        usages.add(new UsageDescriptor(key));
      }
    }
    return usages;
  }
}

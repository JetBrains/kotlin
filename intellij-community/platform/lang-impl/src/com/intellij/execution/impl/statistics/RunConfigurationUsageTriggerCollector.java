// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl.statistics;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

public class RunConfigurationUsageTriggerCollector {

  public static void trigger(@NotNull Project project, @NotNull ConfigurationFactory factory, @NotNull Executor executor) {
    final FeatureUsageData data = new FeatureUsageData().addProject(project);
    final String key = AbstractRunConfigurationTypeUsagesCollector.toReportedId(factory, data);
    if (StringUtil.isNotEmpty(key)) {
      final PluginInfo info = PluginInfoDetectorKt.getPluginInfo(executor.getClass());
      data.addData("executor", info.isSafeToReport() ? executor.getId() : "UNKNOWN");

      FUCounterUsageLogger.getInstance().logEvent(project, "run.configuration.exec", key, data);
    }
  }
}

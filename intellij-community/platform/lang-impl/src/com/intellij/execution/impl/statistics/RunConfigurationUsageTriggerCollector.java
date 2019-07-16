// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl.statistics;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType;
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext;
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomWhiteListRule;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.execution.impl.statistics.RunConfigurationTypeUsagesCollector.FACTORY_FIELD;

public class RunConfigurationUsageTriggerCollector {

  public static void trigger(@NotNull Project project, @NotNull ConfigurationFactory factory, @NotNull Executor executor) {
    final ConfigurationType configurationType = factory.getType();
    final String eventId = RunConfigurationTypeUsagesCollector.toConfigurationId(configurationType);
    final FeatureUsageData data = new FeatureUsageData().addProject(project).addExecutor(executor);
    if (configurationType.getConfigurationFactories().length > 1) {
      data.addData(FACTORY_FIELD, factory.getId());
    }
    FUCounterUsageLogger.getInstance().logEvent(project, "run.configuration.exec", eventId, data);
  }

  public static class RunConfigurationExecutorUtilValidator extends CustomWhiteListRule {

    @Override
    public boolean acceptRuleId(@Nullable String ruleId) {
      return "run_config_executor".equals(ruleId);
    }

    @NotNull
    @Override
    protected ValidationResultType doValidate(@NotNull String data, @NotNull EventContext context) {
      for (Executor executor : Executor.EXECUTOR_EXTENSION_NAME.getExtensions()) {
        if (StringUtil.equals(executor.getId(), data)) {
          final PluginInfo info = PluginInfoDetectorKt.getPluginInfo(executor.getClass());
          return info.isSafeToReport() ? ValidationResultType.ACCEPTED : ValidationResultType.THIRD_PARTY;
        }
      }
      return ValidationResultType.REJECTED;
    }
  }
}

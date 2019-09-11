// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl.statistics;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.UnknownConfigurationType;
import com.intellij.internal.statistic.beans.MetricEvent;
import com.intellij.internal.statistic.beans.MetricEventFactoryKt;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType;
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext;
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomWhiteListRule;
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.UIUtil;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.CancellablePromise;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class RunConfigurationTypeUsagesCollector extends ProjectUsagesCollector {
  private static final String ID_FIELD = "id";
  private static final String FACTORY_FIELD = "factory";

  @NotNull
  @Override
  public String getGroupId() {
    return "run.configuration.type";
  }

  @Override
  public int getVersion() {
    return 5;
  }

  @NotNull
  @Override
  public CancellablePromise<Set<MetricEvent>> getMetrics(@NotNull Project project, @NotNull ProgressIndicator indicator) {
    AsyncPromise<Set<MetricEvent>> result = new AsyncPromise<>();
    UIUtil.invokeLaterIfNeeded(() -> {
      try {
        TObjectIntHashMap<Template> templates = new TObjectIntHashMap<>();
        if (project.isDisposed()) {
          result.setResult(Collections.emptySet());
          return;
        }
        RunManager runManager = RunManager.getInstance(project);
        for (RunnerAndConfigurationSettings settings : runManager.getAllSettings()) {
          ProgressManager.checkCanceled();
          RunConfiguration runConfiguration = settings.getConfiguration();
          final ConfigurationFactory configurationFactory = runConfiguration.getFactory();
          if (configurationFactory == null) {
            // not realistic
            continue;
          }

          final ConfigurationType configurationType = configurationFactory.getType();
          final FeatureUsageData data = newFeatureUsageData(configurationType, configurationFactory);
          fillSettings(data, settings, runConfiguration);
          final Template template = new Template("configured.in.project", data);
          if (templates.containsKey(template)) {
            templates.increment(template);
          }
          else {
            templates.put(template, 1);
          }
        }
        Set<MetricEvent> metrics = new HashSet<>();
        templates.forEachEntry((template, value) -> {
          metrics.add(template.createMetricEvent(value));
          return true;
        });
        result.setResult(metrics);
      }
      catch (Throwable t) {
        result.setError(t);
        throw t;
      }
    });
    return result;
  }

  @NotNull
  public static FeatureUsageData newFeatureUsageData(@NotNull ConfigurationType configuration, @Nullable ConfigurationFactory factory) {
    final String id = configuration instanceof UnknownConfigurationType ? "unknown" : configuration.getId();
    final FeatureUsageData data = new FeatureUsageData().addData(ID_FIELD, id);
    if (factory != null && configuration.getConfigurationFactories().length > 1) {
      data.addData(FACTORY_FIELD, factory.getId());
    }
    return data;
  }

  private static void fillSettings(@NotNull FeatureUsageData data,
                                   @NotNull RunnerAndConfigurationSettings settings,
                                   @NotNull RunConfiguration runConfiguration) {
    data.addData("shared", settings.isShared()).
      addData("edit_before_run", settings.isEditBeforeRun()).
      addData("activate_before_run", settings.isActivateToolWindowBeforeRun()).
      addData("parallel", runConfiguration.isAllowRunningInParallel()).
      addData("temporary", settings.isTemporary());
  }

  private static class Template {
    private final String myKey;
    private final FeatureUsageData myData;

    private Template(String key, FeatureUsageData data) {
      myKey = key;
      myData = data;
    }

    @NotNull
    private MetricEvent createMetricEvent(int count) {
      return MetricEventFactoryKt.newCounterMetric(myKey, count, myData);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Template template = (Template)o;
      return Objects.equals(myKey, template.myKey) &&
             Objects.equals(myData, template.myData);
    }

    @Override
    public int hashCode() {
      return Objects.hash(myKey, myData);
    }
  }

  public static class RunConfigurationUtilValidator extends CustomWhiteListRule {
    @Override
    public boolean acceptRuleId(@Nullable String ruleId) {
      return "run_config_id".equals(ruleId) || "run_config_factory".equals(ruleId);
    }

    @NotNull
    @Override
    protected ValidationResultType doValidate(@NotNull String data, @NotNull EventContext context) {
      if (isThirdPartyValue(data) || "unknown".equals(data)) return ValidationResultType.ACCEPTED;

      final String configurationId = getEventDataField(context, ID_FIELD);
      final String factoryId = getEventDataField(context, FACTORY_FIELD);
      if (configurationId == null) {
        return ValidationResultType.REJECTED;
      }

      if (StringUtil.equals(data, configurationId) || StringUtil.equals(data, factoryId)) {
        final Pair<ConfigurationType, ConfigurationFactory> configurationAndFactory =
          findConfigurationAndFactory(configurationId, factoryId);

        final ConfigurationType configuration = configurationAndFactory.getFirst();
        final ConfigurationFactory factory = configurationAndFactory.getSecond();
        if (configuration != null && (StringUtil.isEmpty(factoryId) || factory != null)) {
          final PluginInfo info = PluginInfoDetectorKt.getPluginInfo(configuration.getClass());
          context.setPluginInfo(info);
          return info.isDevelopedByJetBrains() ? ValidationResultType.ACCEPTED : ValidationResultType.THIRD_PARTY;
        }
      }
      return ValidationResultType.REJECTED;
    }

    @NotNull
    private static Pair<ConfigurationType, ConfigurationFactory> findConfigurationAndFactory(@NotNull String configurationId,
                                                                                             @Nullable String factoryId) {
      final ConfigurationType configuration = findRunConfigurationById(configurationId);
      if (configuration == null) {
        return Pair.empty();
      }

      final ConfigurationFactory factory = StringUtil.isEmpty(factoryId) ? null : findFactoryById(configuration, factoryId);
      return Pair.create(configuration, factory);
    }

    @Nullable
    private static ConfigurationType findRunConfigurationById(@NotNull String configuration) {
      final ConfigurationType[] types = ConfigurationType.CONFIGURATION_TYPE_EP.getExtensions();
      for (ConfigurationType type : types) {
        if (StringUtil.equals(type.getId(), configuration)) {
          return type;
        }
      }
      return null;
    }

    @Nullable
    private static ConfigurationFactory findFactoryById(@NotNull ConfigurationType configuration, @NotNull String factoryId) {
      for (ConfigurationFactory factory : configuration.getConfigurationFactories()) {
        if (StringUtil.equals(factory.getId(), factoryId)) {
          return factory;
        }
      }
      return null;
    }
  }
}

// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl.statistics;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.UnknownConfigurationType;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType;
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext;
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomUtilsWhiteListRule;
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractRunConfigurationTypeUsagesCollector extends ProjectUsagesCollector {
  private static final String DEFAULT_ID = "third.party";

  protected abstract boolean isApplicable(@NotNull RunManager runManager, @NotNull RunnerAndConfigurationSettings settings);

  @NotNull
  @Override
  public Set<UsageDescriptor> getUsages(@NotNull Project project) {
    final TObjectIntHashMap<Template> templates = new TObjectIntHashMap<>();
    ApplicationManager.getApplication().invokeAndWait(() -> {
      if (project.isDisposed()) return;
      final RunManager runManager = RunManager.getInstance(project);
      for (RunnerAndConfigurationSettings settings : runManager.getAllSettings()) {
        RunConfiguration runConfiguration = settings.getConfiguration();
        if (isApplicable(runManager, settings)) {
          final ConfigurationFactory configurationFactory = runConfiguration.getFactory();
          if (configurationFactory == null) {
            // not realistic
            continue;
          }

          final FeatureUsageData data = new FeatureUsageData();
          final String key = toReportedId(configurationFactory, data);
          if (StringUtil.isNotEmpty(key)) {
            final Template template = new Template(key, addContext(data, settings, runConfiguration));
            if (templates.containsKey(template)) {
              templates.increment(template);
            }
            else {
              templates.put(template, 1);
            }
          }
        }
      }
    });

    final Set<UsageDescriptor> result = new HashSet<>();
    templates.forEachEntry((template, value) -> result.add(template.createUsageDescriptor(value)));
    return result;
  }

  @Nullable
  public static String toReportedId(@NotNull ConfigurationFactory factory, @NotNull FeatureUsageData data) {
    final ConfigurationType configurationType = factory.getType();
    if (configurationType instanceof UnknownConfigurationType) {
      return null;
    }

    final PluginInfo info = PluginInfoDetectorKt.getPluginInfo(configurationType.getClass());
    data.addPluginInfo(info);

    if (!info.isDevelopedByJetBrains()) {
      return DEFAULT_ID;
    }
    final StringBuilder keyBuilder = new StringBuilder();
    keyBuilder.append(configurationType.getId());
    if (configurationType.getConfigurationFactories().length > 1) {
      keyBuilder.append("/").append(factory.getId());
    }
    return keyBuilder.toString();
  }

  private static FeatureUsageData addContext(@NotNull FeatureUsageData data,
                                             @NotNull RunnerAndConfigurationSettings settings,
                                             @NotNull RunConfiguration runConfiguration) {
    return data.
      addData("shared", settings.isShared()).
      addData("edit_before_run", settings.isEditBeforeRun()).
      addData("activate_before_run", settings.isActivateToolWindowBeforeRun()).
      addData("parallel", runConfiguration.isAllowRunningInParallel());
  }

  private static class Template {
    private final String myKey;
    private final FeatureUsageData myData;

    private Template(String key, FeatureUsageData data) {
      myKey = key;
      myData = data;
    }

    private UsageDescriptor createUsageDescriptor(int count) {
      return new UsageDescriptor(myKey, count, myData);
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

  public static class RunConfigurationUtilValidator extends CustomUtilsWhiteListRule {

    @Override
    public boolean acceptRuleId(@Nullable String ruleId) {
      return "run_config".equals(ruleId);
    }

    @NotNull
    @Override
    protected ValidationResultType doValidate(@NotNull String data, @NotNull EventContext context) {
      if ("third.party".equals(data)) return ValidationResultType.ACCEPTED;

      final String[] split = data.split("/");
      if (split.length == 1 || split.length == 2) {
        final ConfigurationType configuration = findRunConfigurationById(split[0].trim());
        if (configuration != null && PluginInfoDetectorKt.getPluginInfo(configuration.getClass()).isDevelopedByJetBrains()) {
          if (split.length == 1) {
            return ValidationResultType.ACCEPTED;
          }

          final String factoryId = split[1].trim();
          final ConfigurationFactory factory = findFactoryById(configuration, factoryId);
          if (factory != null) {
            return ValidationResultType.ACCEPTED;
          }
        }
      }
      return ValidationResultType.REJECTED;
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
    private static ConfigurationFactory findFactoryById(ConfigurationType configuration, String factoryId) {
      for (ConfigurationFactory factory : configuration.getConfigurationFactories()) {
        if (StringUtil.equals(factory.getId(), factoryId)) {
          return factory;
        }
      }
      return null;
    }
  }
}

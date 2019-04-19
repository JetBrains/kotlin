// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public abstract class AbstractExternalSystemRunConfigurationProducer extends RunConfigurationProducer<ExternalSystemRunConfiguration> {
  /**
   * @deprecated Override {@link #getConfigurationFactory()}.
   */
  @Deprecated
  public AbstractExternalSystemRunConfigurationProducer(@NotNull AbstractExternalSystemTaskConfigurationType type) {
    super(type);
  }

  protected AbstractExternalSystemRunConfigurationProducer() {
    super(true);
  }

  @Override
  public boolean shouldReplace(@NotNull ConfigurationFromContext self, @NotNull ConfigurationFromContext other) {
    return true;
  }

  @Override
  protected boolean setupConfigurationFromContext(ExternalSystemRunConfiguration configuration,
                                                  ConfigurationContext context,
                                                  Ref<PsiElement> sourceElement) {
    Project project = getProjectFromContext(context);
    if (project == null) return false;

    ExternalSystemTaskExecutionSettings contextTaskExecutionSettings = getTaskSettingsFromContext(context);
    if (contextTaskExecutionSettings == null) return false;

    ExternalSystemTaskExecutionSettings taskExecutionSettings = configuration.getSettings();
    if (!contextTaskExecutionSettings.getExternalSystemId().equals(taskExecutionSettings.getExternalSystemId())) {
      return false;
    }

    taskExecutionSettings.setExternalProjectPath(contextTaskExecutionSettings.getExternalProjectPath());
    taskExecutionSettings.setTaskNames(contextTaskExecutionSettings.getTaskNames());
    configuration.setName(AbstractExternalSystemTaskConfigurationType.generateName(project, taskExecutionSettings));
    return true;
  }

  @Override
  public boolean isConfigurationFromContext(ExternalSystemRunConfiguration configuration, ConfigurationContext context) {
    Project project = getProjectFromContext(context);
    if (project == null) return false;

    ExternalSystemTaskExecutionSettings contextTaskExecutionSettings = getTaskSettingsFromContext(context);
    if (contextTaskExecutionSettings == null) return false;

    ExternalSystemTaskExecutionSettings taskExecutionSettings = configuration.getSettings();
    if (!contextTaskExecutionSettings.getExternalSystemId().equals(taskExecutionSettings.getExternalSystemId())) {
      return false;
    }
    if (!StringUtil.equals(contextTaskExecutionSettings.getExternalProjectPath(), taskExecutionSettings.getExternalProjectPath())) {
      return false;
    }
    if (!contextTaskExecutionSettings.getTaskNames().equals(taskExecutionSettings.getTaskNames())) return false;
    return true;
  }

  @Nullable
  private static ExternalSystemTaskExecutionSettings getTaskSettingsFromContext(ConfigurationContext context) {
    final Location contextLocation = context.getLocation();
    if (!(contextLocation instanceof ExternalSystemTaskLocation)) {
      return null;
    }
    return ((ExternalSystemTaskLocation)contextLocation).getTaskInfo().getSettings();
  }

  @Nullable
  private static Project getProjectFromContext(ConfigurationContext context) {
    final Location contextLocation = context.getLocation();
    if (!(contextLocation instanceof ExternalSystemTaskLocation)) {
      return null;
    }
    return contextLocation.getProject();
  }
}
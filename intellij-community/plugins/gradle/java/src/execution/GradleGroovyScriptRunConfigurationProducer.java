// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.List;

import static org.jetbrains.plugins.gradle.execution.GradleRunnerUtil.*;

/**
 * @author Vladislav.Soroka
 */
final class GradleGroovyScriptRunConfigurationProducer extends LazyRunConfigurationProducer<ExternalSystemRunConfiguration> {
  @NotNull
  @Override
  public ConfigurationFactory getConfigurationFactory() {
    return GradleExternalTaskConfigurationType.getInstance().getFactory();
  }

  @Override
  protected boolean setupConfigurationFromContext(ExternalSystemRunConfiguration configuration,
                                                  ConfigurationContext context,
                                                  Ref<PsiElement> sourceElement) {
    ExternalSystemTaskExecutionSettings taskExecutionSettings = configuration.getSettings();
    if (!GradleConstants.SYSTEM_ID.equals(taskExecutionSettings.getExternalSystemId())) return false;

    final Location contextLocation = context.getLocation();
    if (!isFromGroovyGradleScript(contextLocation)) return false;

    final Module module = context.getModule();
    if (module == null) return false;

    String projectPath = resolveProjectPath(module);
    if (projectPath == null) {
      VirtualFile virtualFile = contextLocation.getVirtualFile();
      projectPath = virtualFile != null ? virtualFile.getPath() : null;
    }
    if (projectPath == null) {
      return false;
    }

    List<String> tasksToRun = getTasksTarget(contextLocation);
    taskExecutionSettings.setExternalProjectPath(projectPath);
    taskExecutionSettings.setTaskNames(tasksToRun);
    configuration.setName(AbstractExternalSystemTaskConfigurationType.generateName(module.getProject(), taskExecutionSettings));
    return true;
  }

  @Override
  public boolean isConfigurationFromContext(ExternalSystemRunConfiguration configuration, ConfigurationContext context) {
    if (configuration == null) return false;
    if (!GradleConstants.SYSTEM_ID.equals(configuration.getSettings().getExternalSystemId())) return false;

    final Location contextLocation = context.getLocation();
    if (!isFromGroovyGradleScript(contextLocation)) return false;

    if (context.getModule() == null) return false;

    final String projectPath = resolveProjectPath(context.getModule());
    if (projectPath == null) return false;
    if (!StringUtil.equals(projectPath, configuration.getSettings().getExternalProjectPath())) {
      return false;
    }

    List<String> tasks = getTasksTarget(contextLocation);
    List<String> taskNames = configuration.getSettings().getTaskNames();
    if (tasks.isEmpty() && taskNames.isEmpty()) {
      return true;
    }

    if (tasks.containsAll(taskNames) && !taskNames.isEmpty()) return true;
    return false;
  }
}

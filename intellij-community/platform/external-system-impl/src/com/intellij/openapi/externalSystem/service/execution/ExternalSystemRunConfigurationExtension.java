// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.options.SettingsEditorGroup;
import org.jdom.Element;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Experimental
public interface ExternalSystemRunConfigurationExtension {
  void readExternal(@NotNull ExternalSystemRunConfiguration configuration, @NotNull Element element);

  void writeExternal(@NotNull ExternalSystemRunConfiguration configuration, @NotNull Element element);

  void appendEditors(@NotNull ExternalSystemRunConfiguration configuration,
                     @NotNull SettingsEditorGroup<ExternalSystemRunConfiguration> group);

  void attachToProcess(@NotNull ExternalSystemRunConfiguration configuration,
                       @NotNull ExternalSystemProcessHandler processHandler,
                       @Nullable RunnerSettings settings);

  void updateVMParameters(@NotNull ExternalSystemRunConfiguration configuration,
                          @NotNull SimpleJavaParameters javaParameters,
                          @Nullable RunnerSettings settings,
                          @NotNull Executor executor);
}

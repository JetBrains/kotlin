/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.service.task;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;

import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public interface GradleTaskManagerExtension {

  ExtensionPointName<GradleTaskManagerExtension> EP_NAME = ExtensionPointName.create("org.jetbrains.plugins.gradle.taskManager");

  /**
   * @deprecated use {@link #executeTasks(ExternalSystemTaskId, List, String, GradleExecutionSettings, String, ExternalSystemTaskNotificationListener)}
   */
  @Deprecated
  default boolean executeTasks(@NotNull final ExternalSystemTaskId id,
                               @NotNull final List<String> taskNames,
                               @NotNull String projectPath,
                               @Nullable final GradleExecutionSettings settings,
                               @NotNull final List<String> vmOptions,
                               @NotNull final List<String> scriptParameters,
                               @Nullable final String jvmParametersSetup,
                               @NotNull final ExternalSystemTaskNotificationListener listener) throws ExternalSystemException {
    return false;
  }

  default boolean executeTasks(@NotNull final ExternalSystemTaskId id,
                               @NotNull final List<String> taskNames,
                               @NotNull String projectPath,
                               @Nullable final GradleExecutionSettings settings,
                               @Nullable final String jvmParametersSetup,
                               @NotNull final ExternalSystemTaskNotificationListener listener) throws ExternalSystemException {
    List<String> vmOptions = settings != null ? settings.getJvmArguments() : Collections.emptyList();
    List<String> arguments = settings != null ? settings.getArguments() : Collections.emptyList();
    return executeTasks(id, taskNames, projectPath, settings, vmOptions, arguments, jvmParametersSetup, listener);
  }

  boolean cancelTask(@NotNull ExternalSystemTaskId id, @NotNull ExternalSystemTaskNotificationListener listener)
    throws ExternalSystemException;
}

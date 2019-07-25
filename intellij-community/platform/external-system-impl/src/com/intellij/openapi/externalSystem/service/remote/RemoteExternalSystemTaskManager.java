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
package com.intellij.openapi.externalSystem.service.remote;

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.RemoteExternalSystemService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Denis Zhdanov
 */
public interface RemoteExternalSystemTaskManager<S extends ExternalSystemExecutionSettings> extends RemoteExternalSystemService<S> {

  /** <a href="http://en.wikipedia.org/wiki/Null_Object_pattern">Null object</a> for {@link RemoteExternalSystemProjectResolverImpl}. */
  RemoteExternalSystemTaskManager<ExternalSystemExecutionSettings> NULL_OBJECT =
    new RemoteExternalSystemTaskManager<ExternalSystemExecutionSettings>() {

      @Override
      public void executeTasks(@NotNull ExternalSystemTaskId id,
                               @NotNull List<String> taskNames,
                               @NotNull String projectPath,
                               @Nullable ExternalSystemExecutionSettings settings,
                               @Nullable String jvmParametersSetup) throws ExternalSystemException {
      }

      @Override
      public boolean cancelTask(@NotNull ExternalSystemTaskId id) throws ExternalSystemException
      {
        return false;
      }

      @Override
      public void setSettings(@NotNull ExternalSystemExecutionSettings settings) {
      }

      @Override
      public void setNotificationListener(@NotNull ExternalSystemTaskNotificationListener notificationListener) {
      }

      @Override
      public boolean isTaskInProgress(@NotNull ExternalSystemTaskId id) {
        return false;
      }

      @NotNull
      @Override
      public Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() {
        return Collections.emptyMap();
      }
    };

  /**
   * @deprecated use {@link RemoteExternalSystemTaskManager#executeTasks(ExternalSystemTaskId, List, String, ExternalSystemExecutionSettings, String)}
   */
  @Deprecated
  default void executeTasks(@NotNull ExternalSystemTaskId id,
                            @NotNull List<String> taskNames,
                            @NotNull String projectPath,
                            @Nullable S settings,
                            @NotNull List<String> vmOptions,
                            @NotNull List<String> scriptParameters,
                            @Nullable String jvmParametersSetup) throws RemoteException, ExternalSystemException {
  }

  default void executeTasks(@NotNull ExternalSystemTaskId id,
                            @NotNull List<String> taskNames,
                            @NotNull String projectPath,
                            @Nullable S settings,
                            @Nullable String jvmParametersSetup) throws RemoteException, ExternalSystemException {
    executeTasks(id, taskNames, projectPath, settings, Collections.emptyList(), Collections.emptyList(), jvmParametersSetup);
  }

  @Override
  boolean cancelTask(@NotNull ExternalSystemTaskId id) throws RemoteException, ExternalSystemException;
}

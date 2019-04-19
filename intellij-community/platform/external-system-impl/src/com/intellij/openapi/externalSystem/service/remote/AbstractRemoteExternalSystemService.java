/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.RemoteExternalSystemService;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author Denis Zhdanov
 */
public abstract class AbstractRemoteExternalSystemService<S extends ExternalSystemExecutionSettings>
  implements RemoteExternalSystemService<S>
{

  private final ConcurrentMap<ExternalSystemTaskType, Set<ExternalSystemTaskId>> myTasksInProgress =
    ContainerUtil.newConcurrentMap();

  private final AtomicReference<S> mySettings = new AtomicReference<>();
  
  private final AtomicReference<ExternalSystemTaskNotificationListener> myListener
    = new AtomicReference<>();

  protected <T> T execute(@NotNull ExternalSystemTaskId id, @NotNull Supplier<? extends T> task) {
    Set<ExternalSystemTaskId> tasks = myTasksInProgress.get(id.getType());
    if (tasks == null) {
      myTasksInProgress.putIfAbsent(id.getType(), new HashSet<>());
      tasks = myTasksInProgress.get(id.getType());
    }
    tasks.add(id);
    try {
      return task.get();
    }
    finally {
      tasks.remove(id);
    }
  }

  @Override
  public void setSettings(@NotNull S settings) {
    mySettings.set(settings);
  }

  @Nullable
  public S getSettings() {
    return mySettings.get();
  }
  
  @Override
  public void setNotificationListener(@NotNull ExternalSystemTaskNotificationListener listener) {
    myListener.set(listener);
  }

  @NotNull
  public ExternalSystemTaskNotificationListener getNotificationListener() {
    return myListener.get();
  }

  @Override
  public boolean isTaskInProgress(@NotNull ExternalSystemTaskId id) {
    Set<ExternalSystemTaskId> tasks = myTasksInProgress.get(id.getType());
    return tasks != null && tasks.contains(id);
  }

  @NotNull
  @Override
  public Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() {
    return myTasksInProgress;
  }
}

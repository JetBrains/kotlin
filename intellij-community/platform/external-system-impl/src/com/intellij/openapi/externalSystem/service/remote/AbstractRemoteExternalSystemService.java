// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.remote;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.RemoteExternalSystemService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    new ConcurrentHashMap<>();

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

  public @Nullable S getSettings() {
    return mySettings.get();
  }

  @Override
  public void setNotificationListener(@NotNull ExternalSystemTaskNotificationListener listener) {
    myListener.set(listener);
  }

  public @NotNull ExternalSystemTaskNotificationListener getNotificationListener() {
    return myListener.get();
  }

  @Override
  public boolean isTaskInProgress(@NotNull ExternalSystemTaskId id) {
    Set<ExternalSystemTaskId> tasks = myTasksInProgress.get(id.getType());
    return tasks != null && tasks.contains(id);
  }

  @Override
  public @NotNull Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() {
    return myTasksInProgress;
  }
}

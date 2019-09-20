// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.remote;

import com.intellij.execution.rmi.RemoteObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class ExternalSystemProgressNotificationManagerImpl extends RemoteObject
  implements ExternalSystemProgressNotificationManager, RemoteExternalSystemProgressNotificationManager {

  private final ConcurrentMap<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>/* EMPTY_SET as a sign of 'all ids' */>
    myListeners
    = ContainerUtil.newConcurrentMap();

  public static ExternalSystemProgressNotificationManagerImpl getInstanceImpl() {
    return (ExternalSystemProgressNotificationManagerImpl)ApplicationManager.getApplication().getService(ExternalSystemProgressNotificationManager.class);
  }

  @Override
  public boolean addNotificationListener(@NotNull ExternalSystemTaskNotificationListener listener) {
    Set<ExternalSystemTaskId> dummy = Collections.emptySet();
    return myListeners.put(listener, dummy) == null;
  }

  @Override
  public boolean addNotificationListener(@NotNull ExternalSystemTaskId taskId, @NotNull ExternalSystemTaskNotificationListener listener) {
    Set<ExternalSystemTaskId> ids = null;
    while (ids == null) {
      if (myListeners.containsKey(listener)) {
        ids = myListeners.get(listener);
      }
      else {
        ids = myListeners.putIfAbsent(listener, ContainerUtil.newConcurrentSet());
      }
    }
    return ids.add(taskId);
  }

  @Override
  public boolean removeNotificationListener(@NotNull ExternalSystemTaskNotificationListener listener) {
    return myListeners.remove(listener) != null;
  }

  @Override
  public void onStart(@NotNull ExternalSystemTaskId id, @NotNull String workingDir) {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      final Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(id)) {
        entry.getKey().onStart(id, workingDir);
      }
    }
  }

  @Override
  public void onStatusChange(@NotNull ExternalSystemTaskNotificationEvent event) {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      final Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(event.getId())) {
        entry.getKey().onStatusChange(event);
      }
    }
  }

  @Override
  public void onTaskOutput(@NotNull ExternalSystemTaskId id, @NotNull String text, boolean stdOut) {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      final Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(id)) {
        entry.getKey().onTaskOutput(id, text, stdOut);
      }
    }
  }

  @Override
  public void onEnd(@NotNull ExternalSystemTaskId id) {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      final Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(id)) {
        entry.getKey().onEnd(id);
      }
    }
  }

  @Override
  public void onSuccess(@NotNull ExternalSystemTaskId id) {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      final Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(id)) {
        entry.getKey().onSuccess(id);
      }
    }
  }

  @Override
  public void onFailure(@NotNull ExternalSystemTaskId id, @NotNull Exception e) {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      final Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(id)) {
        entry.getKey().onFailure(id, e);
      }
    }
  }

  @Override
  public void beforeCancel(@NotNull ExternalSystemTaskId id) {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      final Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(id)) {
        entry.getKey().beforeCancel(id);
      }
    }
  }

  @Override
  public void onCancel(ExternalSystemTaskId id) {
    for (Map.Entry<ExternalSystemTaskNotificationListener, Set<ExternalSystemTaskId>> entry : myListeners.entrySet()) {
      final Set<ExternalSystemTaskId> ids = entry.getValue();
      if (Collections.EMPTY_SET == ids || ids.contains(id)) {
        entry.getKey().onCancel(id);
      }
    }
  }
}

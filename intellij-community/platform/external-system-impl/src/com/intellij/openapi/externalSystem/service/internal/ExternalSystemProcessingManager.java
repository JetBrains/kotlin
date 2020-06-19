// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.internal;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.*;
import com.intellij.openapi.externalSystem.service.ExternalSystemFacadeManager;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.Alarm;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Provides external system tasks monitoring and management facilities.
 * <p/>
 * Thread-safe.
 */
public final class ExternalSystemProcessingManager implements ExternalSystemTaskNotificationListener, Disposable {

  /**
   * We receive information about the tasks being enqueued to the slave processes which work directly with external systems here.
   * However, there is a possible situation when particular task has been sent to execution but remote side has not been responding
   * for a while. There at least two possible explanations then:
   * <pre>
   * <ul>
   *   <li>the task is still in progress (e.g. great number of libraries is being downloaded);</li>
   *   <li>remote side has fallen (uncaught exception; manual slave process kill etc);</li>
   * </ul>
   * </pre>
   * We need to distinguish between them, so, we perform 'task pings' if any task is executed too long. Current constant holds
   * criteria of 'too long execution'.
   */
  private static final long TOO_LONG_EXECUTION_MS = TimeUnit.SECONDS.toMillis(10);

  private final @NotNull ConcurrentMap<ExternalSystemTaskId, Long> myTasksInProgress = new ConcurrentHashMap<>();
  private final @NotNull ConcurrentMap<ExternalSystemTaskId, ExternalSystemTask> myTasksDetails = new ConcurrentHashMap<>();
  private final @NotNull Alarm myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);

  private final @NotNull ExternalSystemFacadeManager myFacadeManager;

  public ExternalSystemProcessingManager() {
    Application app = ApplicationManager.getApplication();
    myFacadeManager = app.getService(ExternalSystemFacadeManager.class);
    if (app.isUnitTestMode()) {
      return;
    }
    app.getService(ExternalSystemProgressNotificationManager.class).addNotificationListener(this, this);
  }

  @Override
  public void dispose() {
    myAlarm.cancelAllRequests();
  }

  /**
   * Allows to check if any task of the given type is being executed at the moment.
   *
   * @param type  target task type
   * @return      {@code true} if any task of the given type is being executed at the moment;
   *              {@code false} otherwise
   */
  public boolean hasTaskOfTypeInProgress(@NotNull ExternalSystemTaskType type, @NotNull Project project) {
    String projectId = ExternalSystemTaskId.getProjectId(project);
    for (ExternalSystemTaskId id : myTasksInProgress.keySet()) {
      if (type.equals(id.getType()) && projectId.equals(id.getIdeProjectId())) {
        return true;
      }
    }
    return false;
  }

  public @Nullable ExternalSystemTask findTask(@NotNull ExternalSystemTaskId id) {
    return myTasksDetails.get(id);
  }

  public @Nullable ExternalSystemTask findTask(@NotNull ExternalSystemTaskType type,
                                               @NotNull ProjectSystemId projectSystemId,
                                               final @NotNull String externalProjectPath) {
    for(ExternalSystemTask task : myTasksDetails.values()) {
      if(task instanceof AbstractExternalSystemTask) {
        AbstractExternalSystemTask externalSystemTask = (AbstractExternalSystemTask)task;
        if(externalSystemTask.getId().getType() == type &&
           externalSystemTask.getExternalSystemId().getId().equals(projectSystemId.getId()) &&
           externalSystemTask.getExternalProjectPath().equals(externalProjectPath)){
          return task;
        }
      }
    }

    return null;
  }

  public @NotNull List<ExternalSystemTask> findTasksOfState(@NotNull ProjectSystemId projectSystemId,
                                                            ExternalSystemTaskState @NotNull ... taskStates) {
    List<ExternalSystemTask> result = new SmartList<>();
    for (ExternalSystemTask task : myTasksDetails.values()) {
      if (task instanceof AbstractExternalSystemTask) {
        AbstractExternalSystemTask externalSystemTask = (AbstractExternalSystemTask)task;
        if (externalSystemTask.getExternalSystemId().getId().equals(projectSystemId.getId()) &&
            ArrayUtil.contains(externalSystemTask.getState(), taskStates)) {
          result.add(task);
        }
      }
    }
    return result;
  }

  public void add(@NotNull ExternalSystemTask task) {
    myTasksDetails.put(task.getId(), task);
  }

  public void release(@NotNull ExternalSystemTaskId id) {
    myTasksDetails.remove(id);
  }

  @Override
  public void onStart(@NotNull ExternalSystemTaskId id, String workingDir) {
    myTasksInProgress.put(id, System.currentTimeMillis() + TOO_LONG_EXECUTION_MS);
    if (myAlarm.isEmpty()) {
      myAlarm.addRequest(() -> update(), TOO_LONG_EXECUTION_MS);
    }
  }

  @Override
  public void onStart(@NotNull ExternalSystemTaskId id) {
    myTasksInProgress.put(id, System.currentTimeMillis() + TOO_LONG_EXECUTION_MS);
  }

  @Override
  public void onStatusChange(@NotNull ExternalSystemTaskNotificationEvent event) {
    myTasksInProgress.put(event.getId(), System.currentTimeMillis() + TOO_LONG_EXECUTION_MS);
  }

  @Override
  public void onTaskOutput(@NotNull ExternalSystemTaskId id, @NotNull String text, boolean stdOut) {
    myTasksInProgress.put(id, System.currentTimeMillis() + TOO_LONG_EXECUTION_MS);
  }

  @Override
  public void onEnd(@NotNull ExternalSystemTaskId id) {
    myTasksInProgress.remove(id);
    if (myTasksInProgress.isEmpty()) {
      myAlarm.cancelAllRequests();
    }
  }

  @Override
  public void onSuccess(@NotNull ExternalSystemTaskId id) {
  }

  @Override
  public void onFailure(@NotNull ExternalSystemTaskId id, @NotNull Exception e) {
  }

  @Override
  public void beforeCancel(@NotNull ExternalSystemTaskId id) {
  }

  @Override
  public void onCancel(@NotNull ExternalSystemTaskId id) {
  }

  public void update() {
    long delay = TOO_LONG_EXECUTION_MS;
    Map<ExternalSystemTaskId, Long> newState = new HashMap<>();

    Map<ExternalSystemTaskId, Long> currentState = new HashMap<>(myTasksInProgress);
    if (currentState.isEmpty()) {
      return;
    }

    for (Map.Entry<ExternalSystemTaskId, Long> entry : currentState.entrySet()) {
      long diff = System.currentTimeMillis() - entry.getValue();
      if (diff > 0) {
        delay = Math.min(delay, diff);
        newState.put(entry.getKey(), entry.getValue());
      }
      else {
        // Perform explicit check on whether the task is still alive.
        if (myFacadeManager.isTaskActive(entry.getKey())) {
          newState.put(entry.getKey(), System.currentTimeMillis() + TOO_LONG_EXECUTION_MS);
        }
      }
    }

    myTasksInProgress.clear();
    myTasksInProgress.putAll(newState);

    if (!newState.isEmpty()) {
      myAlarm.cancelAllRequests();
      myAlarm.addRequest(() -> update(), delay);
    }
  }
}

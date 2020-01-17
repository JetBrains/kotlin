package com.intellij.openapi.externalSystem.service.internal;

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import org.jetbrains.annotations.NotNull;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

/**
 * Represents a service that exposes information about the tasks being processed. 
 * 
 * @author Denis Zhdanov
 */
public interface  ExternalSystemTaskAware {

  /**
   * Allows to check if current service executes the target task.
   *
   * @param id  target task's id
   * @return    {@code true} if a task with the given id is executed at the moment by the current service;
   *            {@code false} otherwise
   * @throws RemoteException      as required by RMI
   */
  boolean isTaskInProgress(@NotNull ExternalSystemTaskId id) throws RemoteException;

  /**
   * Allows to cancel the target task by the current service.
   *
   *
   * @param id  target task's id
   * @return    {@code true} if a task was successfully canceled;
   *            {@code false} otherwise
   * @throws RemoteException      as required by RMI
   */
  boolean cancelTask(@NotNull ExternalSystemTaskId id) throws RemoteException;

  /**
   * Allows to ask current service for all tasks being executed at the moment.  
   *
   * @return      ids of all tasks being executed at the moment grouped by type
   * @throws RemoteException      as required by RMI
   */
  @NotNull
  Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() throws RemoteException;
}

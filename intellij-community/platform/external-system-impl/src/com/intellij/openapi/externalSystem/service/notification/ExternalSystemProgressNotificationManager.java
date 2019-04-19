package com.intellij.openapi.externalSystem.service.notification;

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;

/**
 * @author Denis Zhdanov
 */
public interface ExternalSystemProgressNotificationManager {

  /**
   * Allows to register given listener to listen events from all tasks.
   * 
   * @param listener  listener to register
   * @return          {@code true} if given listener was not registered before for the given key;
   *                  {@code false} otherwise
   */
  boolean addNotificationListener(@NotNull ExternalSystemTaskNotificationListener listener);
  
  /**
   * Allows to register given listener within the current manager for listening events from the task with the target id. 
   * 
   * @param taskId    target task's id
   * @param listener  listener to register
   * @return          {@code true} if given listener was not registered before for the given key;
   *                  {@code false} otherwise
   */
  boolean addNotificationListener(@NotNull ExternalSystemTaskId taskId, @NotNull ExternalSystemTaskNotificationListener listener);

  /**
   * Allows to de-register given listener from the current manager
   *
   * @param listener  listener to de-register
   * @return          {@code true} if given listener was successfully de-registered;
   *                  {@code false} if given listener was not registered before
   */
  boolean removeNotificationListener(@NotNull ExternalSystemTaskNotificationListener listener);
}

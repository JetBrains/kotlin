package com.intellij.openapi.externalSystem.service;

import com.intellij.openapi.externalSystem.service.internal.ExternalSystemTaskAware;
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import org.jetbrains.annotations.NotNull;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Generic interface with common functionality for all remote services that work with external system.
 * 
 * @author Denis Zhdanov
 */
public interface RemoteExternalSystemService<S extends ExternalSystemExecutionSettings> extends Remote, ExternalSystemTaskAware {

  /**
   * Provides the service settings to use.
   * 
   * @param settings  settings to use
   * @throws RemoteException      as required by RMI
   */
  void setSettings(@NotNull S settings) throws RemoteException;

  /**
   * Allows to define notification callback to use within the current service
   * 
   * @param notificationListener  notification listener to use with the current service
   * @throws RemoteException      as required by RMI
   */
  void setNotificationListener(@NotNull ExternalSystemTaskNotificationListener notificationListener) throws RemoteException;
}

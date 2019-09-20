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
package com.intellij.openapi.externalSystem.service.remote.wrapper;

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.RemoteExternalSystemService;
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import org.jetbrains.annotations.NotNull;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

/**
 * @author Denis Zhdanov
 */
public abstract class AbstractRemoteExternalSystemServiceWrapper<S extends ExternalSystemExecutionSettings, T extends RemoteExternalSystemService<S>>
  implements RemoteExternalSystemService<S>
{

  @NotNull private final T myDelegate;

  public AbstractRemoteExternalSystemServiceWrapper(@NotNull T delegate) {
    myDelegate = delegate;
  }

  @Override
  public void setSettings(@NotNull S settings) throws RemoteException {
    myDelegate.setSettings(settings);
  }

  @Override
  public void setNotificationListener(@NotNull ExternalSystemTaskNotificationListener notificationListener) throws RemoteException {
    myDelegate.setNotificationListener(notificationListener);
  }

  @Override
  public boolean isTaskInProgress(@NotNull ExternalSystemTaskId id) throws RemoteException {
    return myDelegate.isTaskInProgress(id);
  }

  @Override
  @NotNull
  public Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() throws RemoteException {
    return myDelegate.getTasksInProgress();
  }

  @NotNull
  public T getDelegate() {
    return myDelegate;
  }
}

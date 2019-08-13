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

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.service.remote.RemoteExternalSystemProgressNotificationManager;
import com.intellij.openapi.externalSystem.service.remote.RemoteExternalSystemTaskManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.rmi.RemoteException;
import java.util.List;

/**
 * @author Denis Zhdanov
 */
public class ExternalSystemTaskManagerWrapper<S extends ExternalSystemExecutionSettings>
  extends AbstractRemoteExternalSystemServiceWrapper<S, RemoteExternalSystemTaskManager<S>>
  implements RemoteExternalSystemTaskManager<S> {

  @NotNull private final RemoteExternalSystemProgressNotificationManager myProgressManager;

  public ExternalSystemTaskManagerWrapper(@NotNull RemoteExternalSystemTaskManager<S> delegate,
                                          @NotNull RemoteExternalSystemProgressNotificationManager progressManager) {
    super(delegate);
    myProgressManager = progressManager;
  }

  @Override
  public void executeTasks(@NotNull ExternalSystemTaskId id,
                           @NotNull List<String> taskNames,
                           @NotNull String projectPath,
                           @Nullable S settings,
                           @Nullable String jvmParametersSetup) throws RemoteException, ExternalSystemException {
    try {
      getDelegate().executeTasks(id, taskNames, projectPath, settings, jvmParametersSetup);
      myProgressManager.onSuccess(id);
    }
    catch (ExternalSystemException e) {
      myProgressManager.onFailure(id, e);
      throw e;
    }
    catch (Exception e) {
      myProgressManager.onFailure(id, e);
      throw new ExternalSystemException(e);
    }
    finally {
      myProgressManager.onEnd(id);
    }
  }

  @Override
  public boolean cancelTask(@NotNull ExternalSystemTaskId id) throws RemoteException, ExternalSystemException {
    return getDelegate().cancelTask(id);
  }
}


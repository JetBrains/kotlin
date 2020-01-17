// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service;

import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.remote.ExternalSystemProgressNotificationManagerImpl;
import com.intellij.openapi.externalSystem.service.remote.wrapper.ExternalSystemFacadeWrapper;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InProcessExternalSystemCommunicationManager implements ExternalSystemCommunicationManager {
  @SuppressWarnings("unchecked")
  @Nullable
  @Override
  public RemoteExternalSystemFacade acquire(@NotNull String id, @NotNull ProjectSystemId externalSystemId) throws Exception {
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    assert manager != null;
    InProcessExternalSystemFacadeImpl result = new InProcessExternalSystemFacadeImpl(manager.getProjectResolverClass(),
                                                                                     manager.getTaskManagerClass());
    result.applyProgressManager(ExternalSystemProgressNotificationManagerImpl.getInstanceImpl());
    return result;
  }

  @Override
  public void release(@NotNull String id, @NotNull ProjectSystemId externalSystemId) {
  }

  @Override
  public boolean isAlive(@NotNull RemoteExternalSystemFacade facade) {
    RemoteExternalSystemFacade toCheck = facade;
    if (facade instanceof ExternalSystemFacadeWrapper) {
      toCheck = ((ExternalSystemFacadeWrapper)facade).getDelegate();
    }
    return toCheck instanceof InProcessExternalSystemFacadeImpl;
  }

  @Override
  public void clear() {
  }
}

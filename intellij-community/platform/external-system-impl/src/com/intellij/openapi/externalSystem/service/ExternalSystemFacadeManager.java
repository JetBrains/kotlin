// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import com.intellij.openapi.externalSystem.service.remote.RemoteExternalSystemProgressNotificationManager;
import com.intellij.openapi.externalSystem.service.remote.wrapper.ExternalSystemFacadeWrapper;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.IntegrationKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Entry point to work with remote {@link RemoteExternalSystemFacade}.
 * <p/>
 * Thread-safe.
 */
public final class ExternalSystemFacadeManager {
  private static final int REMOTE_FAIL_RECOVERY_ATTEMPTS_NUMBER = 3;

  private final ConcurrentMap<IntegrationKey, RemoteExternalSystemFacade> myFacadeWrappers = ContainerUtil.newConcurrentMap();

  private final Map<IntegrationKey, Pair<RemoteExternalSystemFacade, ExternalSystemExecutionSettings>> myRemoteFacades
    = ContainerUtil.newConcurrentMap();

  @NotNull private final Lock          myLock                   = new ReentrantLock();

  @NotNull private final RemoteExternalSystemProgressNotificationManager myProgressManager;
  @NotNull private final RemoteExternalSystemCommunicationManager        myRemoteCommunicationManager;
  @NotNull private final InProcessExternalSystemCommunicationManager     myInProcessCommunicationManager;

  public ExternalSystemFacadeManager() {
    Application app = ApplicationManager.getApplication();

    myProgressManager = (RemoteExternalSystemProgressNotificationManager)app.getService(ExternalSystemProgressNotificationManager.class);
    myRemoteCommunicationManager = app.getService(RemoteExternalSystemCommunicationManager.class);
    myInProcessCommunicationManager = app.getService(InProcessExternalSystemCommunicationManager.class);
  }

  @NotNull
  private static Project findProject(@NotNull IntegrationKey key) {
    final ProjectManager projectManager = ProjectManager.getInstance();
    for (Project project : projectManager.getOpenProjects()) {
      if (key.getIdeProjectName().equals(project.getName()) && key.getIdeProjectLocationHash().equals(project.getLocationHash())) {
        return project;
      }
    }
    return projectManager.getDefaultProject();
  }

  public void onProjectRename(@NotNull String oldName, @NotNull String newName) {
    onProjectRename(myFacadeWrappers, oldName, newName);
    onProjectRename(myRemoteFacades, oldName, newName);
  }

  private static <V> void onProjectRename(@NotNull Map<IntegrationKey, V> data,
                                          @NotNull String oldName,
                                          @NotNull String newName)
  {
    Set<IntegrationKey> keys = new HashSet<>(data.keySet());
    for (IntegrationKey key : keys) {
      if (!key.getIdeProjectName().equals(oldName)) {
        continue;
      }
      IntegrationKey newKey = new IntegrationKey(newName,
                                                 key.getIdeProjectLocationHash(),
                                                 key.getExternalSystemId(),
                                                 key.getExternalProjectConfigPath());
      V value = data.get(key);
      data.put(newKey, value);
      data.remove(key);
      if (value instanceof Consumer) {
        //noinspection unchecked
        ((Consumer)value).consume(newKey);
      }
    }
  }

  /**
   * @return external system api facade to use
   * @throws Exception    in case of inability to return the facade
   */
  @NotNull
  public RemoteExternalSystemFacade getFacade(@Nullable Project project,
                                              @NotNull String externalProjectPath,
                                              @NotNull ProjectSystemId externalSystemId) {
    if (project == null) {
      project = ProjectManager.getInstance().getDefaultProject();
    }
    IntegrationKey key = new IntegrationKey(project, externalSystemId, externalProjectPath);
    final RemoteExternalSystemFacade facade = myFacadeWrappers.get(key);
    if (facade == null) {
      final RemoteExternalSystemFacade newFacade = (RemoteExternalSystemFacade)Proxy.newProxyInstance(
        ExternalSystemFacadeManager.class.getClassLoader(), new Class[]{RemoteExternalSystemFacade.class, Consumer.class},
        new MyHandler(key)
      );
      myFacadeWrappers.putIfAbsent(key, newFacade);
    }
    return myFacadeWrappers.get(key);
  }

  public Object doInvoke(@NotNull IntegrationKey key, @NotNull Project project, Method method, Object[] args, int invocationNumber)
    throws Throwable
  {
    RemoteExternalSystemFacade facade = doGetFacade(key, project);
    try {
      return method.invoke(facade, args);
    }
    catch (InvocationTargetException e) {
      if (e.getTargetException() instanceof RemoteException && invocationNumber > 0) {
        Thread.sleep(1000);
        return doInvoke(key, project, method, args, invocationNumber - 1);
      }
      else {
        throw e;
      }
    }
  }

  public ExternalSystemCommunicationManager getCommunicationManager(@NotNull ProjectSystemId externalSystemId) {
    final boolean currentInProcess = ExternalSystemApiUtil.isInProcessMode(externalSystemId);
    return currentInProcess ? myInProcessCommunicationManager : myRemoteCommunicationManager;
  }

  @NotNull
  private RemoteExternalSystemFacade doGetFacade(@NotNull IntegrationKey key, @NotNull Project project) throws Exception {
    final boolean currentInProcess = ExternalSystemApiUtil.isInProcessMode(key.getExternalSystemId());
    final ExternalSystemCommunicationManager myCommunicationManager = currentInProcess ? myInProcessCommunicationManager : myRemoteCommunicationManager;

    ExternalSystemManager manager = ExternalSystemApiUtil.getManager(key.getExternalSystemId());
    if (project.isDisposed() || manager == null) {
      return RemoteExternalSystemFacade.NULL_OBJECT;
    }
    Pair<RemoteExternalSystemFacade, ExternalSystemExecutionSettings> pair = myRemoteFacades.get(key);
    if (pair != null && prepare(myCommunicationManager, project, key, pair)) {
      return pair.first;
    }

    myLock.lock();
    try {
      pair = myRemoteFacades.get(key);
      if (pair != null && prepare(myCommunicationManager, project, key, pair)) {
        return pair.first;
      }
      if (pair != null) {
        myFacadeWrappers.clear();
        myRemoteFacades.clear();
      }
      return doCreateFacade(key, project, myCommunicationManager);
    }
    finally {
      myLock.unlock();
    }
  }

  @SuppressWarnings("unchecked")
  @NotNull
  private RemoteExternalSystemFacade doCreateFacade(@NotNull IntegrationKey key, @NotNull Project project,
                                                    @NotNull ExternalSystemCommunicationManager communicationManager) throws Exception {
    final RemoteExternalSystemFacade facade = communicationManager.acquire(key.getExternalProjectConfigPath(), key.getExternalSystemId());
    if (facade == null) {
      throw new IllegalStateException("Can't obtain facade to working with external api at the remote process. Project: " + project);
    }
    Disposer.register(project, new Disposable() {
      @Override
      public void dispose() {
        myFacadeWrappers.clear();
        myRemoteFacades.clear();
      }
    });
    final RemoteExternalSystemFacade result = new ExternalSystemFacadeWrapper(facade, myProgressManager);
    ExternalSystemExecutionSettings settings
      = ExternalSystemApiUtil.getExecutionSettings(project, key.getExternalProjectConfigPath(), key.getExternalSystemId());
    Pair<RemoteExternalSystemFacade, ExternalSystemExecutionSettings> newPair = Pair.create(result, settings);
    myRemoteFacades.put(key, newPair);
    result.applySettings(newPair.second);
    return result;
  }

  @SuppressWarnings("unchecked")
  private boolean prepare(@NotNull ExternalSystemCommunicationManager communicationManager,
                          @NotNull Project project, @NotNull IntegrationKey key,
                          @NotNull Pair<RemoteExternalSystemFacade, ExternalSystemExecutionSettings> pair)
  {
     if (!communicationManager.isAlive(pair.first)) {
      return false;
    }
    try {
      ExternalSystemExecutionSettings currentSettings
        = ExternalSystemApiUtil.getExecutionSettings(project, key.getExternalProjectConfigPath(), key.getExternalSystemId());
      if (!currentSettings.equals(pair.second)) {
        pair.first.applySettings(currentSettings);
        myRemoteFacades.put(key, Pair.create(pair.first, currentSettings));
      }
      return true;
    }
    catch (RemoteException e) {
      return false;
    }
  }

  public boolean isTaskActive(@NotNull ExternalSystemTaskId id) {
    Map<IntegrationKey, Pair<RemoteExternalSystemFacade, ExternalSystemExecutionSettings>> copy
      = new HashMap<>(myRemoteFacades);
    for (Map.Entry<IntegrationKey, Pair<RemoteExternalSystemFacade, ExternalSystemExecutionSettings>> entry : copy.entrySet()) {
      try {
        if (entry.getValue().first.isTaskInProgress(id)) {
          return true;
        }
      }
      catch (RemoteException e) {
        myLock.lock();
        try {
          myRemoteFacades.remove(entry.getKey());
          myFacadeWrappers.remove(entry.getKey());
        }
        finally {
          myLock.unlock();
        }
      }
    }
    return false;
  }

  private class MyHandler implements InvocationHandler {

    @NotNull private final AtomicReference<IntegrationKey> myKey = new AtomicReference<>();

    MyHandler(@NotNull IntegrationKey key) {
      myKey.set(key);
    }

    @Nullable
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if ("consume".equals(method.getName())) {
        myKey.set((IntegrationKey)args[0]);
        return null;
      }
      Project project = findProject(myKey.get());
      return doInvoke(myKey.get(), project, method, args, REMOTE_FAIL_RECOVERY_ATTEMPTS_NUMBER);
    }
  }
}

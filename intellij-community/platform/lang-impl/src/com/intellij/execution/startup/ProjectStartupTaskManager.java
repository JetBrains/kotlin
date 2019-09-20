// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.startup;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectLifecycleListener;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class ProjectStartupTaskManager {
  public static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.logOnlyGroup("Project Startup Tasks Messages");
  @NonNls public static final String PREFIX = "Project Startup Tasks: ";
  private final Project myProject;
  private final ProjectStartupSharedConfiguration myShared;
  private final ProjectStartupLocalConfiguration myLocal;

  public static ProjectStartupTaskManager getInstance(@NotNull Project project) {
    return project.getService(ProjectStartupTaskManager.class);
  }

  public ProjectStartupTaskManager(@NotNull Project project) {
    myProject = project;
    myShared = myProject.getService(ProjectStartupSharedConfiguration.class);
    myLocal = myProject.getService(ProjectStartupLocalConfiguration.class);
    verifyState();
  }

  // This method is called from a post-startup activity so the 'post-startup activities done'
  // event always happens after this method is done executing
  public void waitForExecutionReady(Runnable runnable) {
    Disposable readyDisposable = Disposer.newDisposable();
    Disposer.register(myProject, readyDisposable);
    myProject.getMessageBus().connect(readyDisposable).subscribe(ProjectLifecycleListener.TOPIC, new ProjectLifecycleListener() {
      @Override
      public void postStartupActivitiesPassed(@NotNull Project project) {
        if (project == myProject) {
          runnable.run();
          Disposer.dispose(readyDisposable);
        }
      }
    });
  }

  private void verifyState() {
    if (myShared.isEmpty()) {
      return;
    }

    final Collection<RunnerAndConfigurationSettings> sharedConfigurations = getSharedConfigurations();
    final List<RunnerAndConfigurationSettings> canNotBeShared = new ArrayList<>();
    final Iterator<RunnerAndConfigurationSettings> iterator = sharedConfigurations.iterator();
    while (iterator.hasNext()) {
      final RunnerAndConfigurationSettings configuration = iterator.next();
      if (!configuration.isShared()) {
        iterator.remove();
        canNotBeShared.add(configuration);
      }
    }
    if (! canNotBeShared.isEmpty()) {
      canNotBeShared.addAll(getLocalConfigurations());
      setStartupConfigurations(sharedConfigurations, canNotBeShared);
    }
  }

  public Collection<RunnerAndConfigurationSettings> getSharedConfigurations() {
    return getConfigurations(myShared);
  }

  public Collection<RunnerAndConfigurationSettings> getLocalConfigurations() {
    return getConfigurations(myLocal);
  }

  private Collection<RunnerAndConfigurationSettings> getConfigurations(ProjectStartupConfigurationBase configuration) {
    if (configuration.isEmpty()) return Collections.emptyList();

    final List<RunnerAndConfigurationSettings> result = new ArrayList<>();
    final List<ProjectStartupConfigurationBase.ConfigurationDescriptor> list = configuration.getList();
    RunManagerImpl runManager = (RunManagerImpl)RunManager.getInstance(myProject);
    for (ProjectStartupConfigurationBase.ConfigurationDescriptor descriptor : list) {
      final RunnerAndConfigurationSettings settings = runManager.getConfigurationById(descriptor.getId());
      if (settings != null && settings.getName().equals(descriptor.getName())) {
        result.add(settings);
      } else {
        NOTIFICATION_GROUP.createNotification(PREFIX + " Run Configuration '" + descriptor.getName() + "' not found, removed from list.",
                                              MessageType.WARNING).notify(myProject);
      }
    }
    return result;
  }

  public void rename(final String oldId, RunnerAndConfigurationSettings settings) {
    if (myShared.rename(oldId, settings)) {
      return;
    }
    myLocal.rename(oldId, settings);

  }

  public void delete(final String id) {
    if (myShared.deleteConfiguration(id)) {
      return;
    }
    myLocal.deleteConfiguration(id);
  }

  public void setStartupConfigurations(final @NotNull Collection<? extends RunnerAndConfigurationSettings> shared,
                                       final @NotNull Collection<? extends RunnerAndConfigurationSettings> local) {
    myShared.setConfigurations(shared);
    myLocal.setConfigurations(local);
  }

  public boolean isEmpty() {
    return myShared.isEmpty() && myLocal.isEmpty();
  }

  public void checkOnChange(RunnerAndConfigurationSettings settings) {
    if (!settings.isShared()) {
      final Collection<RunnerAndConfigurationSettings> sharedConfigurations = getSharedConfigurations();
      if (sharedConfigurations.remove(settings)) {
        final List<RunnerAndConfigurationSettings> localConfigurations = new ArrayList<>(getLocalConfigurations());
        localConfigurations.add(settings);
        setStartupConfigurations(sharedConfigurations, localConfigurations);

        NOTIFICATION_GROUP.createNotification(PREFIX + " configuration was made \"not shared\", since included Run Configuration '" +
                                              settings.getName() + "' is not shared.", MessageType.WARNING).notify(myProject);
      }
    }
  }
}

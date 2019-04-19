/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.execution.startup;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Irina.Chernushina on 8/19/2015.
 */
public class ProjectStartupTaskManager {
  public static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.logOnlyGroup("Project Startup Tasks Messages");
  @NonNls public static final String PREFIX = "Project Startup Tasks: ";
  private final Project myProject;
  private final ProjectStartupSharedConfiguration myShared;
  private final ProjectStartupLocalConfiguration myLocal;

  public static ProjectStartupTaskManager getInstance(@NotNull final Project project) {
    return ServiceManager.getService(project, ProjectStartupTaskManager.class);
  }

  public ProjectStartupTaskManager(Project project, ProjectStartupSharedConfiguration shared, ProjectStartupLocalConfiguration local) {
    myProject = project;
    myShared = shared;
    myLocal = local;
    verifyState();
  }

  private void verifyState() {
    if (! myShared.isEmpty()) {
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
    if (myShared.rename(oldId, settings)) return;
    myLocal.rename(oldId, settings);
  }

  public void delete(final String id) {
    if (myShared.deleteConfiguration(id)) return;
    myLocal.deleteConfiguration(id);
  }

  public void setStartupConfigurations(final @NotNull Collection<RunnerAndConfigurationSettings> shared,
                                       final @NotNull Collection<RunnerAndConfigurationSettings> local) {
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

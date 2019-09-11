// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.autoimport;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListenerAdapter;
import com.intellij.openapi.externalSystem.util.CompoundParallelOperationTrace;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


public class ExternalSystemProjectsWatcherImpl extends ExternalSystemTaskNotificationListenerAdapter
  implements ExternalSystemProjectsWatcher, Disposable {

  private final Project project;
  private final CompoundParallelOperationTrace<ExternalSystemTaskId> trackerDisableOperation = new CompoundParallelOperationTrace<>();

  private static final ExtensionPointName<Contributor> EP_NAME =
    ExtensionPointName.create("com.intellij.externalProjectWatcherContributor");

  public ExternalSystemProjectsWatcherImpl(Project project) {
    this.project = project;
    ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
    trackerDisableOperation.beforeOperation(() -> projectTracker.setDisabled(true));
    trackerDisableOperation.afterOperation(() -> projectTracker.setDisabled(false));
  }

  @Override
  public void markDirtyAllExternalProjects() {
    ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
    findProjectSettings().forEach(it -> projectTracker.markDirty(it));
    for (Contributor contributor : EP_NAME.getExtensions()) {
      contributor.markDirtyAllExternalProjects(project);
    }
    projectTracker.scheduleProjectRefresh();
  }

  @Override
  public void markDirty(Module module) {
    ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
    String projectPath = ExternalSystemApiUtil.getExternalProjectPath(module);
    findProjectSettings().stream()
      .filter(it -> it.getExternalProjectPath() == projectPath)
      .forEach(it -> projectTracker.markDirty(it));
    for (Contributor contributor : EP_NAME.getExtensions()) {
      contributor.markDirty(module);
    }
    projectTracker.scheduleProjectRefresh();
  }

  @Override
  public void markDirty(String projectPath) {
    ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
    findProjectSettings().stream()
      .filter(it -> it.getExternalProjectPath() == projectPath)
      .forEach(it -> projectTracker.markDirty(it));
    for (Contributor contributor : EP_NAME.getExtensions()) {
      contributor.markDirty(projectPath);
    }
    projectTracker.scheduleProjectRefresh();
  }

  public synchronized void start() {
    ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
    for (ExternalSystemManager<?, ?, ?, ?, ?> manager : ExternalSystemApiUtil.getAllManagers()) {
      ProjectSystemId systemId = manager.getSystemId();
      AbstractExternalSystemSettings<?, ?, ?> settings = manager.getSettingsProvider().fun(project);
      if (!(manager instanceof ExternalSystemAutoImportAware)) continue;
      ExternalSystemAutoImportAware autoImportAware = (ExternalSystemAutoImportAware)manager;
      for (ExternalProjectSettings projectSettings : settings.getLinkedProjectsSettings()) {
        ExternalSystemProjectId projectId = new ExternalSystemProjectId(systemId, projectSettings.getExternalProjectPath());
        projectTracker.register(new ProjectAware(project, projectId, autoImportAware));
      }
      ProjectSettingsListener settingsListener = new ProjectSettingsListener(systemId, autoImportAware);
      ExternalSystemApiUtil.subscribe(project, systemId, settingsListener);
      manager.getLocalSettingsProvider().fun(project).getExternalConfigModificationStamps();
    }
    ServiceManager.getService(ExternalSystemProgressNotificationManager.class).addNotificationListener(this);
  }

  private List<ExternalSystemProjectId> findProjectSettings() {
    List<ExternalSystemProjectId> list = new ArrayList<>();
    ExternalSystemManager.EP_NAME.forEachExtensionSafe(manager -> {
      ProjectSystemId systemId = manager.getSystemId();
      Collection<? extends ExternalProjectSettings> linkedProjectsSettings =
        manager.getSettingsProvider().fun(project).getLinkedProjectsSettings();
      for (ExternalProjectSettings settings : linkedProjectsSettings) {
        String externalProjectPath = settings.getExternalProjectPath();
        list.add(new ExternalSystemProjectId(systemId, externalProjectPath));
      }
    });
    return list;
  }

  @Override
  public void dispose() {
    stop();
  }

  public synchronized void stop() {
    ServiceManager.getService(ExternalSystemProgressNotificationManager.class).removeNotificationListener(this);
  }

  /**
   * Disable project auto import and notification, but doesn't disable file watching.
   * Automatically enabled after project sync
   */
  public void disableAutoUpdate() {
    trackerDisableOperation.startOperation();
  }

  @Override
  public void onStart(@NotNull ExternalSystemTaskId id, String workingDir) {
    trackerDisableOperation.startTask(id);
  }

  @Override
  public void onEnd(@NotNull ExternalSystemTaskId id) {
    trackerDisableOperation.finishTask(id);
  }

  public interface Contributor {

    void markDirtyAllExternalProjects(@NotNull Project project);

    void markDirty(@NotNull Module module);

    default void markDirty(@NotNull String projectPath) {}
  }

  private class ProjectSettingsListener extends ExternalSystemSettingsListenerAdapter<ExternalProjectSettings> {

    private final ProjectSystemId systemId;
    private final ExternalSystemAutoImportAware autoImportAware;

    ProjectSettingsListener(ProjectSystemId systemId, ExternalSystemAutoImportAware autoImportAware) {
      this.systemId = systemId;
      this.autoImportAware = autoImportAware;
    }

    @Override
    public void onProjectsLinked(Collection<ExternalProjectSettings> settings) {
      ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
      for (ExternalProjectSettings projectSettings : settings) {
        String externalProjectPath = projectSettings.getExternalProjectPath();
        ExternalSystemProjectId id = new ExternalSystemProjectId(systemId, externalProjectPath);
        projectTracker.register(new ProjectAware(project, id, autoImportAware));
      }
    }

    @Override
    public void onProjectsUnlinked(Set<String> linkedProjectPaths) {
      ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
      for (String linkedProjectPath : linkedProjectPaths) {
        projectTracker.remove(new ExternalSystemProjectId(systemId, linkedProjectPath));
      }
    }
  }
}

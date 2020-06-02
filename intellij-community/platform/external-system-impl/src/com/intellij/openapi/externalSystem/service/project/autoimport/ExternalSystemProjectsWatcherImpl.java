// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.autoimport;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class ExternalSystemProjectsWatcherImpl implements ExternalSystemProjectsWatcher {

  @NotNull
  private final Project project;

  private static final ExtensionPointName<Contributor> EP_NAME =
    ExtensionPointName.create("com.intellij.externalProjectWatcherContributor");

  public ExternalSystemProjectsWatcherImpl(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public void markDirtyAllExternalProjects() {
    ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
    List<ExternalSystemProjectId> projectSettings = findAllProjectSettings();
    ApplicationManager.getApplication().invokeLater(() -> {
      projectSettings.forEach(it -> projectTracker.markDirty(it));
      for (Contributor contributor : EP_NAME.getExtensions()) {
        contributor.markDirtyAllExternalProjects(project);
      }
      projectTracker.scheduleProjectRefresh();
    }, project.getDisposed());
  }

  @Override
  public void markDirty(@NotNull Module module) {
    ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
    String projectPath = ExternalSystemApiUtil.getExternalProjectPath(module);
    List<ExternalSystemProjectId> projectSettings = findAllProjectSettings();
    ApplicationManager.getApplication().invokeLater(() -> {
      projectSettings.stream()
        .filter(it -> it.getExternalProjectPath().equals(projectPath))
        .forEach(it -> projectTracker.markDirty(it));
      for (Contributor contributor : EP_NAME.getExtensions()) {
        contributor.markDirty(module);
      }
      projectTracker.scheduleProjectRefresh();
    }, module.getDisposed());
  }

  @Override
  public void markDirty(@NotNull String projectPath) {
    ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
    List<ExternalSystemProjectId> projectSettings = findAllProjectSettings();
    ApplicationManager.getApplication().invokeLater(() -> {
      projectSettings.stream()
        .filter(it -> it.getExternalProjectPath().equals(projectPath))
        .forEach(it -> projectTracker.markDirty(it));
      for (Contributor contributor : EP_NAME.getExtensions()) {
        contributor.markDirty(projectPath);
      }
      projectTracker.scheduleProjectRefresh();
    }, project.getDisposed());
  }

  private List<ExternalSystemProjectId> findAllProjectSettings() {
    List<ExternalSystemProjectId> list = new ArrayList<>();
    ExternalSystemManager.EP_NAME.forEachExtensionSafe(manager -> {
      ProjectSystemId systemId = manager.getSystemId();
      Collection<? extends ExternalProjectSettings> linkedProjectsSettings =
        manager.getSettingsProvider().fun(project).getLinkedProjectsSettings();
      for (ExternalProjectSettings settings : linkedProjectsSettings) {
        String externalProjectPath = settings.getExternalProjectPath();
        if (externalProjectPath == null) continue;
        list.add(new ExternalSystemProjectId(systemId, externalProjectPath));
      }
    });
    return list;
  }

  public interface Contributor {

    void markDirtyAllExternalProjects(@NotNull Project project);

    void markDirty(@NotNull Module module);

    default void markDirty(@NotNull String projectPath) {}
  }
}

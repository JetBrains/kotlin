// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.autoimport;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.ExtensionPointUtil;
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListenerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

@ApiStatus.Internal
class ExternalProjectsSettingsWatcher implements ExternalSystemSettingsListenerEx {
  @Override
  public void onProjectsLoaded(
    @NotNull Project project,
    @NotNull ExternalSystemManager<?, ?, ?, ?, ?> manager,
    @NotNull Collection<? extends ExternalProjectSettings> settings
  ) {
    if (!(manager instanceof ExternalSystemAutoImportAware)) return;
    ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
    ProjectSystemId systemId = manager.getSystemId();
    for (ExternalProjectSettings projectSettings : settings) {
      String externalProjectPath = projectSettings.getExternalProjectPath();
      projectTracker.activate(new ExternalSystemProjectId(systemId, externalProjectPath));
    }
  }

  @Override
  public void onProjectsLinked(
    @NotNull Project project,
    @NotNull ExternalSystemManager<?, ?, ?, ?, ?> manager,
    @NotNull Collection<? extends ExternalProjectSettings> settings
  ) {
    if (!(manager instanceof ExternalSystemAutoImportAware)) return;
    Disposable extensionDisposable = ExtensionPointUtil.createExtensionDisposable(manager, ExternalSystemManager.EP_NAME);
    Disposer.register(project, extensionDisposable);
    ExternalSystemAutoImportAware autoImportAware = (ExternalSystemAutoImportAware)manager;
    ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
    ProjectSystemId systemId = manager.getSystemId();
    for (ExternalProjectSettings projectSettings : settings) {
      String externalProjectPath = projectSettings.getExternalProjectPath();
      ExternalSystemProjectId id = new ExternalSystemProjectId(systemId, externalProjectPath);
      projectTracker.register(new ProjectAware(project, id, autoImportAware), extensionDisposable);
    }
  }

  @Override
  public void onProjectsUnlinked(
    @NotNull Project project,
    @NotNull ExternalSystemManager<?, ?, ?, ?, ?> manager,
    @NotNull Set<String> linkedProjectPaths
  ) {
    if (!(manager instanceof ExternalSystemAutoImportAware)) return;
    ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
    ProjectSystemId systemId = manager.getSystemId();
    for (String linkedProjectPath : linkedProjectPaths) {
      projectTracker.remove(new ExternalSystemProjectId(systemId, linkedProjectPath));
    }
  }
}
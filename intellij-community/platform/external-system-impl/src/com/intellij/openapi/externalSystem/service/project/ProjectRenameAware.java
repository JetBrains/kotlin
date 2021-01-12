// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.service.ExternalSystemFacadeManager;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListenerAdapter;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * We need to avoid memory leaks on ide project rename. This class is responsible for that.
 *
 * @author Denis Zhdanov
 */
public final class ProjectRenameAware {

  public static void beAware(@NotNull Project project) {
    final ExternalSystemFacadeManager facadeManager = ServiceManager.getService(ExternalSystemFacadeManager.class);
    for (ExternalSystemManager<?, ?, ?, ?, ?> manager : ExternalSystemApiUtil.getAllManagers()) {
      AbstractExternalSystemSettings settings = manager.getSettingsProvider().fun(project);
      //noinspection unchecked
      settings.subscribe(new ExternalSystemSettingsListenerAdapter() {
        @Override
        public void onProjectRenamed(@NotNull String oldName, @NotNull String newName) {
          facadeManager.onProjectRename(oldName, newName);
        }
      });
    }
  }
}

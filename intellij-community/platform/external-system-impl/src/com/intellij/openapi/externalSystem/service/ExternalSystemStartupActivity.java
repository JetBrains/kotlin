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
package com.intellij.openapi.externalSystem.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.service.project.ProjectRenameAware;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.service.ui.ExternalToolWindowManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.DisposeAwareRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 */
public class ExternalSystemStartupActivity implements StartupActivity, DumbAware {

  @Override
  public void runActivity(@NotNull final Project project) {
    ExternalProjectsManagerImpl.getInstance(project).init();
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }
    Runnable task = () -> {
      for (ExternalSystemManager<?, ?, ?, ?, ?> manager: ExternalSystemApiUtil.getAllManagers()) {
        if (manager instanceof StartupActivity) {
          ((StartupActivity)manager).runActivity(project);
        }
      }
      if (project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) != Boolean.TRUE) {
        for (ExternalSystemManager manager: ExternalSystemManager.EP_NAME.getExtensions()) {
          final boolean isNewProject = project.getUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT) == Boolean.TRUE;
          if (isNewProject) {
            ExternalSystemUtil.refreshProjects(new ImportSpecBuilder(project, manager.getSystemId())
                                                 .createDirectoriesForEmptyContentRoots());
          }
        }
      }
      ExternalToolWindowManager.handle(project);
      ProjectRenameAware.beAware(project);
    };

    DumbService.getInstance(project).runWhenSmart(DisposeAwareRunnable.create(task, project));
  }
}

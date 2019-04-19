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
public class ProjectRenameAware {
  
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

// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.data;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ExternalProject;

import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemConstants.BUILTIN_SERVICE_ORDER)
public class ExternalProjectDataService extends AbstractProjectDataService<ExternalProject, Project> {
  private static final Logger LOG = Logger.getInstance(ExternalProjectDataService.class);

  @NotNull public static final Key<ExternalProject> KEY = Key.create(ExternalProject.class, ProjectKeys.TASK.getProcessingWeight() + 1);

  @NotNull
  @Override
  public Key<ExternalProject> getTargetDataKey() {
    return KEY;
  }

  @Override
  public void importData(@NotNull final Collection<DataNode<ExternalProject>> toImport,
                         @Nullable ProjectData projectData,
                         @NotNull final Project project,
                         @NotNull IdeModifiableModelsProvider modelsProvider) {
    if(toImport.isEmpty()) return;
    if (toImport.size() != 1) {
      throw new IllegalArgumentException(
        String.format("Expected to get a single external project but got %d: %s", toImport.size(), toImport));
    }
    ExternalProjectDataCache.getInstance(project).saveExternalProject(toImport.iterator().next().getData());
  }
}

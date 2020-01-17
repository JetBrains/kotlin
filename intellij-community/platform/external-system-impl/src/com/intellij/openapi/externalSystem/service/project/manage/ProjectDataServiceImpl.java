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
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.util.DisposeAwareProjectChange;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Denis Zhdanov
 */
@Order(ExternalSystemConstants.BUILTIN_PROJECT_DATA_SERVICE_ORDER)
public class ProjectDataServiceImpl extends AbstractProjectDataService<ProjectData, Project> {
  
  @NotNull
  @Override
  public Key<ProjectData> getTargetDataKey() {
    return ProjectKeys.PROJECT;
  }

  @Override
  public void importData(@NotNull Collection<DataNode<ProjectData>> toImport,
                         @Nullable ProjectData projectData,
                         @NotNull final Project project,
                         @NotNull IdeModifiableModelsProvider modelsProvider) {
    // root project can be marked as ignored
    if(toImport.isEmpty()) return;

    if (toImport.size() != 1) {
      throw new IllegalArgumentException(String.format("Expected to get a single project but got %d: %s", toImport.size(), toImport));
    }
    DataNode<ProjectData> node = toImport.iterator().next();
    assert projectData == node.getData();

    if (!ExternalSystemApiUtil.isOneToOneMapping(project, node.getData(), modelsProvider.getModules())) {
      return;
    }
    
    if (!project.getName().equals(projectData.getInternalName())) {
      renameProject(projectData.getInternalName(), projectData.getOwner(), project);
    }
  }

  private static void renameProject(@NotNull final String newName,
                                    @NotNull final ProjectSystemId externalSystemId,
                                    @NotNull final Project project)
  {
    if (!(project instanceof ProjectEx) || newName.equals(project.getName())) {
      return;
    }
    ExternalSystemApiUtil.executeProjectChangeAction(true, new DisposeAwareProjectChange(project) {
      @Override
      public void execute() {
        String oldName = project.getName();
        ((ProjectEx)project).setProjectName(newName);
        ExternalSystemApiUtil.getSettings(project, externalSystemId).getPublisher().onProjectRenamed(oldName, newName);
      }
    });
  }

}

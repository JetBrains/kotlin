/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.execution.cmd.CommandLineCompletionProvider;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.TextAccessor;
import com.intellij.util.containers.ContainerUtil;
import groovyjarjarcommonscli.Options;
import icons.ExternalSystemIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class TaskCompletionProvider extends CommandLineCompletionProvider {

  private volatile List<LookupElement> myCachedElements;
  private volatile String myCachedWorkingDir;
  private final Project myProject;
  private final ProjectSystemId mySystemId;
  private final TextAccessor myProjectPathAccessor;


  public TaskCompletionProvider(@NotNull Project project,
                                @NotNull ProjectSystemId externalSystemId,
                                @NotNull TextAccessor workDirectoryField) {
    this(project, externalSystemId, workDirectoryField, new Options());
  }

  public TaskCompletionProvider(@NotNull Project project,
                                @NotNull ProjectSystemId externalSystemId,
                                @NotNull TextAccessor workDirectoryField,
                                @NotNull Options options) {
    super(options);
    myProject = project;
    mySystemId = externalSystemId;
    myProjectPathAccessor = workDirectoryField;
  }

  @Override
  protected void addArgumentVariants(@NotNull CompletionResultSet result) {
    List<LookupElement> cachedElements = myCachedElements;
    final String projectPath = myProjectPathAccessor.getText();
    if (cachedElements == null || !StringUtil.equals(myCachedWorkingDir, projectPath)) {
      final ExternalProjectSettings linkedProjectSettings =
        ExternalSystemApiUtil.getSettings(myProject, mySystemId).getLinkedProjectSettings(projectPath);
      if (linkedProjectSettings == null) return;

      final ExternalProjectInfo projectData =
        ProjectDataManager.getInstance().getExternalProjectData(myProject, mySystemId, linkedProjectSettings.getExternalProjectPath());

      if (projectData == null || projectData.getExternalProjectStructure() == null) return;

      cachedElements = ContainerUtil.newArrayList(getVariants(projectData.getExternalProjectStructure(), projectPath));

      myCachedElements = cachedElements;
      myCachedWorkingDir = projectPath;
    }
    result.addAllElements(cachedElements);
  }

  protected List<LookupElement> getVariants(@NotNull final DataNode<ProjectData> projectDataNode, @NotNull final String modulePath) {
    final DataNode<ModuleData> moduleDataNode = findModuleDataNode(projectDataNode, modulePath);
    if (moduleDataNode == null) {
      return Collections.emptyList();
    }

    final Collection<DataNode<TaskData>> tasks = ExternalSystemApiUtil.getChildren(moduleDataNode, ProjectKeys.TASK);
    List<LookupElement> elements = ContainerUtil.newArrayListWithCapacity(tasks.size());
    for (DataNode<TaskData> taskDataNode : tasks) {
      elements.add(LookupElementBuilder.create(taskDataNode.getData().getName()).withIcon(ExternalSystemIcons.Task));
    }
    return elements;
  }

  @Nullable
  public static DataNode<ModuleData> findModuleDataNode(@NotNull final DataNode<ProjectData> projectDataNode,
                                                        @NotNull final String projectPath) {
    final DataNode<?> node =
      ExternalSystemApiUtil.findFirstRecursively(projectDataNode, node1 -> node1.getKey().equals(ProjectKeys.MODULE) &&
                                                                           node1.getData() instanceof ModuleData &&
                                                                           ((ModuleData)node1
                                                                             .getData()).getLinkedExternalProjectPath().equals(projectPath));

    //noinspection unchecked
    return (DataNode<ModuleData>)node;
  }

}


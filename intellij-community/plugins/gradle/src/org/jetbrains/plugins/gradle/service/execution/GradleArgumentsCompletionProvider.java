// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.execution;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.execution.TaskCompletionProvider;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.TextAccessor;
import icons.ExternalSystemIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.execution.cmd.GradleCommandLineOptionsProvider;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class GradleArgumentsCompletionProvider extends TaskCompletionProvider {

  public GradleArgumentsCompletionProvider(@NotNull Project project, @NotNull TextAccessor workDirectoryField) {
    super(project, GradleConstants.SYSTEM_ID, workDirectoryField, GradleCommandLineOptionsProvider.getSupportedOptions());
  }

  @Override
  protected List<LookupElement> getVariants(@NotNull final DataNode<ProjectData> projectDataNode, @NotNull final String modulePath) {
    final DataNode<ModuleData> moduleDataNode = findModuleDataNode(projectDataNode, modulePath);
    if (moduleDataNode == null) {
      return Collections.emptyList();
    }

    final ModuleData moduleData = moduleDataNode.getData();
    final boolean isRoot = projectDataNode.getData().getLinkedExternalProjectPath().equals(moduleData.getLinkedExternalProjectPath());
    final Collection<DataNode<TaskData>> tasks = ExternalSystemApiUtil.getChildren(moduleDataNode, ProjectKeys.TASK);
    List<LookupElement> elements = new ArrayList<>(tasks.size());

    for (DataNode<TaskData> taskDataNode : tasks) {
      final TaskData taskData = taskDataNode.getData();
      elements.add(LookupElementBuilder.create(taskData.getName()).withIcon(ExternalSystemIcons.Task));
      if (!taskData.isInherited()) {
        elements.add(LookupElementBuilder.create((isRoot ? ':' : moduleData.getId() + ':') + taskData.getName())
                       .withIcon(ExternalSystemIcons.Task));
      }
    }
    return elements;
  }
}

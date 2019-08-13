// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.testFramework.LightPlatformTestCase;
import groovyjarjarcommonscli.Option;
import groovyjarjarcommonscli.Options;
import one.util.streamex.StreamEx;
import org.jetbrains.plugins.gradle.service.execution.cmd.GradleCommandLineOptionsProvider;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.List;
import java.util.Objects;

public class GradleRunAnythingProviderTest extends LightPlatformTestCase {

  public void testGetValues() {
    GradleRunAnythingProvider provider = new GradleRunAnythingProvider();
    Project project = getProject();
    DataContext dataContext = SimpleDataContext.getProjectContext(project);
    assertEmpty("Do not provide values no gradle project linked", provider.getValues(dataContext, "gradle"));

    linkProject(project, "projectPath", "projectA");

    // check tasks completions
    String[] allTasks = {"build", "buildNeeded", "aTask", ":sub:build", ":sub:aTask"};
    List<String> allTasksVariants = StreamEx.of(allTasks).map(s -> "gradle " + s).toList();
    assertSameElements(provider.getValues(dataContext, "gradle"), allTasksVariants);
    assertSameElements(provider.getValues(dataContext, "gradle "), allTasksVariants);
    assertSameElements(provider.getValues(dataContext, "gradle build"), allTasksVariants);

    // check CLI arguments completions
    Options supportedOptions = GradleCommandLineOptionsProvider.getSupportedOptions();
    //noinspection unchecked
    assertSameElements(provider.getValues(dataContext, "gradle -"), supportedOptions.getOptions()
      .stream()
      .map(o -> o instanceof Option ? ((Option)o).getOpt() : null)
      .filter(Objects::nonNull)
      .map(o -> "gradle -" + o)
      .toArray());

    //noinspection unchecked
    assertSameElements(provider.getValues(dataContext, "gradle --"),
                       supportedOptions.getOptions()
                         .stream()
                         .map(o -> o instanceof Option ? ((Option)o).getLongOpt() : null)
                         .filter(Objects::nonNull)
                         .map(o -> "gradle --" + o)
                         .toArray());

    // check completions for multiple linked projects
    linkProject(project, "projectPath2", "projectB");

    assertSameElements(provider.getValues(dataContext, "gradle"), "gradle projectB", "gradle projectA");
    List<String> allTasksProjectBVariants = StreamEx.of(allTasks).map(s -> "gradle projectB " + s).toList();
    assertSameElements(provider.getValues(dataContext, "gradle projectB "), allTasksProjectBVariants);
  }

  private static void linkProject(Project project, String projectPath, String projectName) {
    GradleProjectSettings projectSettings = new GradleProjectSettings();
    projectSettings.setExternalProjectPath(projectPath);
    DataNode<ProjectData> projectDataNode =
      new DataNode<>(ProjectKeys.PROJECT, new ProjectData(GradleConstants.SYSTEM_ID, projectName, projectPath, projectPath), null);

    DataNode<ModuleData> moduleNode = projectDataNode
      .createChild(ProjectKeys.MODULE, new ModuleData(projectName, GradleConstants.SYSTEM_ID, "", projectName, projectPath, projectPath));

    addTask(moduleNode, "build", "build");
    addTask(moduleNode, "buildNeeded", "buildNeeded");
    addTask(moduleNode, "aTask", "aTask");

    DataNode<ModuleData> subModuleNode = projectDataNode
      .createChild(ProjectKeys.MODULE, new ModuleData(":sub", GradleConstants.SYSTEM_ID, "", "test", projectPath, projectPath));

    addTask(subModuleNode, "build", "build");
    addTask(subModuleNode, "aTask", ":sub:aTask");

    ExternalProjectsManagerImpl.getInstance(project)
      .updateExternalProjectData(new InternalExternalProjectInfo(GradleConstants.SYSTEM_ID, projectPath, projectDataNode));

    GradleSettings.getInstance(project).linkProject(projectSettings);

    ProjectRootManager.getInstance(project).incModificationCount();
  }

  private static void addTask(DataNode<ModuleData> moduleNode, String taskName, String taskPath) {
    moduleNode.createChild(ProjectKeys.TASK, new TaskData(GradleConstants.SYSTEM_ID, taskName, taskPath, ""));
  }
}
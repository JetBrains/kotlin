// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.project.PerformanceTrace;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class TracedProjectResolverExtension extends AbstractProjectResolverExtension {
  private final PerformanceTrace myTrace;

  public TracedProjectResolverExtension(@NotNull final GradleProjectResolverExtension nextExtension,
                                        @NotNull final PerformanceTrace trace) {
    setNext(nextExtension);
    myTrace = trace;
  }

  @NotNull
  @Override
  public ProjectData createProject() {
    final long startTime = System.currentTimeMillis();
    final ProjectData project = super.createProject();
    myTrace.logPerformance("Resolver chain createProject", System.currentTimeMillis() - startTime);
    return project;
  }

  @Override
  public void populateProjectExtraModels(@NotNull IdeaProject gradleProject, @NotNull DataNode<ProjectData> ideProject) {
    final long startTime = System.currentTimeMillis();
    super.populateProjectExtraModels(gradleProject, ideProject);
    myTrace.logPerformance("Resolver chain populateProjectExtraModels project " + gradleProject.getName(),
                           System.currentTimeMillis() - startTime);
  }

  @NotNull
  @Override
  public DataNode<ModuleData> createModule(@NotNull IdeaModule gradleModule, @NotNull DataNode<ProjectData> projectDataNode) {
    final long startTime = System.currentTimeMillis();
    final DataNode<ModuleData> module = super.createModule(gradleModule, projectDataNode);
    myTrace.logPerformance("Resolver chain createModule module " + gradleModule.getName(),
                           System.currentTimeMillis() - startTime);
    return module;
  }

  @Override
  public void populateModuleExtraModels(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
    final long startTime = System.currentTimeMillis();
    super.populateModuleExtraModels(gradleModule, ideModule);
    myTrace.logPerformance("Resolver chain populateModuleExtraModels module " + gradleModule.getName(),
                           System.currentTimeMillis() - startTime);
  }

  @Override
  public void populateModuleContentRoots(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
    final long startTime = System.currentTimeMillis();
    super.populateModuleContentRoots(gradleModule, ideModule);
    myTrace.logPerformance("Resolver chain populateModuleContentRoots module " + gradleModule.getName(),
                           System.currentTimeMillis() - startTime);
  }

  @Override
  public void populateModuleCompileOutputSettings(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
    final long startTime = System.currentTimeMillis();
    super.populateModuleCompileOutputSettings(gradleModule, ideModule);
    myTrace.logPerformance("Resolver chain populateModuleCompileOutputSettings module " + gradleModule.getName(),
                           System.currentTimeMillis() - startTime);
  }

  @Override
  public void populateModuleDependencies(@NotNull IdeaModule gradleModule,
                                         @NotNull DataNode<ModuleData> ideModule,
                                         @NotNull DataNode<ProjectData> ideProject) {
    final long startTime = System.currentTimeMillis();
    super.populateModuleDependencies(gradleModule, ideModule, ideProject);
    myTrace.logPerformance("Resolver chain populateModuleDependencies module " + gradleModule.getName(),
                           System.currentTimeMillis() - startTime);
  }

  @NotNull
  @Override
  public Collection<TaskData> populateModuleTasks(@NotNull IdeaModule gradleModule,
                                                  @NotNull DataNode<ModuleData> ideModule,
                                                  @NotNull DataNode<ProjectData> ideProject)
    throws IllegalArgumentException, IllegalStateException {
    final long startTime = System.currentTimeMillis();
    final Collection<TaskData> data = super.populateModuleTasks(gradleModule, ideModule, ideProject);
    myTrace.logPerformance("Resolver chain populateModuleTasks module " + gradleModule.getName(),
                           System.currentTimeMillis() - startTime);
    return data;
  }
}

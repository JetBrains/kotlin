/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.util.Pair;
import com.intellij.util.Consumer;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ClassSetProjectImportModelProvider;
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * {@link AbstractProjectResolverExtension} provides dummy implementation of Gradle project resolver.
 *
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemConstants.UNORDERED)
public abstract class AbstractProjectResolverExtension implements GradleProjectResolverExtension {

  @NotNull protected ProjectResolverContext resolverCtx;
  @NotNull protected GradleProjectResolverExtension nextResolver;

  @Override
  public void setProjectResolverContext(@NotNull ProjectResolverContext projectResolverContext) {
    resolverCtx = projectResolverContext;
  }

  @Override
  public void setNext(@NotNull GradleProjectResolverExtension next) {
    // there always should be at least gradle basic resolver further in the chain
    //noinspection ConstantConditions
    assert next != null;
    nextResolver = next;
  }

  @Nullable
  @Override
  public GradleProjectResolverExtension getNext() {
    return nextResolver;
  }

  @Override
  public void populateProjectExtraModels(@NotNull IdeaProject gradleProject, @NotNull DataNode<ProjectData> ideProject) {
    nextResolver.populateProjectExtraModels(gradleProject, ideProject);
  }

  @Nullable
  @Override
  public DataNode<ModuleData> createModule(@NotNull IdeaModule gradleModule, @NotNull DataNode<ProjectData> projectDataNode) {
    return nextResolver.createModule(gradleModule, projectDataNode);
  }

  @Override
  public void populateModuleExtraModels(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
    nextResolver.populateModuleExtraModels(gradleModule, ideModule);
  }

  @Override
  public void populateModuleContentRoots(@NotNull IdeaModule gradleModule,
                                         @NotNull DataNode<ModuleData> ideModule) {
    nextResolver.populateModuleContentRoots(gradleModule, ideModule);
  }


  @Override
  public void populateModuleCompileOutputSettings(@NotNull IdeaModule gradleModule,
                                                  @NotNull DataNode<ModuleData> ideModule) {
    nextResolver.populateModuleCompileOutputSettings(gradleModule, ideModule);
  }

  @Override
  public void populateModuleDependencies(@NotNull IdeaModule gradleModule,
                                         @NotNull DataNode<ModuleData> ideModule,
                                         @NotNull DataNode<ProjectData> ideProject) {
    nextResolver.populateModuleDependencies(gradleModule, ideModule, ideProject);
  }

  @NotNull
  @Override
  public Collection<TaskData> populateModuleTasks(@NotNull IdeaModule gradleModule,
                                                  @NotNull DataNode<ModuleData> ideModule,
                                                  @NotNull DataNode<ProjectData> ideProject)
    throws IllegalArgumentException, IllegalStateException {
    return nextResolver.populateModuleTasks(gradleModule, ideModule, ideProject);
  }

  @NotNull
  @Override
  public Set<Class> getExtraProjectModelClasses() {
    return Collections.emptySet();
  }

  @Nullable
  @Override
  public ProjectImportModelProvider getModelProvider() {
    Set<Class> projectModelClasses = getExtraProjectModelClasses();
    return projectModelClasses.isEmpty() ? null : new ClassSetProjectImportModelProvider(projectModelClasses);
  }

  @NotNull
  @Override
  public Set<Class> getToolingExtensionsClasses() {
    return Collections.emptySet();
  }

  @NotNull
  @Override
  public List<Pair<String, String>> getExtraJvmArgs() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public List<String> getExtraCommandLineArgs() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public ExternalSystemException getUserFriendlyError(@Nullable BuildEnvironment buildEnvironment,
                                                      @NotNull Throwable error,
                                                      @NotNull String projectPath,
                                                      @Nullable String buildFilePath) {
    return nextResolver.getUserFriendlyError(buildEnvironment, error, projectPath, buildFilePath);
  }

  @Override
  public void enhanceRemoteProcessing(@NotNull SimpleJavaParameters parameters) throws ExecutionException {
  }

  @Override
  public void preImportCheck() {
  }

  @Override
  public void enhanceTaskProcessing(@NotNull List<String> taskNames,
                                    @Nullable String jvmParametersSetup,
                                    @NotNull Consumer<String> initScriptConsumer) {
  }
}

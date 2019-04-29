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

import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.ParametersEnhancer;
import com.intellij.openapi.util.Pair;
import com.intellij.util.Consumer;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.GradleManager;
import org.jetbrains.plugins.gradle.model.ProjectImportExtraModelProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Allows to enhance {@link GradleProjectResolver} processing.
 * <p/>
 * Every extension is expected to have a no-args constructor because they are used at external process and we need a simple way
 * to instantiate it.
 *
 * @author Denis Zhdanov, Vladislav Soroka
 * @see GradleManager#enhanceRemoteProcessing(SimpleJavaParameters)   sample enhanceParameters() implementation
 */
public interface GradleProjectResolverExtension extends ParametersEnhancer {

  ExtensionPointName<GradleProjectResolverExtension> EP_NAME = ExtensionPointName.create("org.jetbrains.plugins.gradle.projectResolve");

  void setProjectResolverContext(@NotNull ProjectResolverContext projectResolverContext);

  void setNext(@NotNull GradleProjectResolverExtension projectResolverExtension);

  @Nullable
  GradleProjectResolverExtension getNext();

  @NotNull
  ProjectData createProject();

  void populateProjectExtraModels(@NotNull IdeaProject gradleProject, @NotNull DataNode<ProjectData> ideProject);

  @NotNull
  DataNode<ModuleData> createModule(@NotNull IdeaModule gradleModule, @NotNull DataNode<ProjectData> projectDataNode);

  /**
   * Populates extra models of the given ide module on the basis of the information provided by {@link org.jetbrains.plugins.gradle.tooling.ModelBuilderService}
   *
   * @param ideModule corresponding module from intellij gradle plugin domain
   */
  void populateModuleExtraModels(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule);

  /**
   * Populates {@link com.intellij.openapi.externalSystem.model.ProjectKeys#CONTENT_ROOT) content roots} of the given ide module on the basis of the information
   * contained at the given gradle module.
   *
   * @param gradleModule holder of the module information received from the gradle tooling api
   * @param ideModule    corresponding module from intellij gradle plugin domain
   * @throws IllegalArgumentException if given gradle module contains invalid data
   */
  void populateModuleContentRoots(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule);

  void populateModuleCompileOutputSettings(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule);

  void populateModuleDependencies(@NotNull IdeaModule gradleModule,
                                  @NotNull DataNode<ModuleData> ideModule,
                                  @NotNull DataNode<ProjectData> ideProject);

  @NotNull
  Collection<TaskData> populateModuleTasks(@NotNull IdeaModule gradleModule,
                                           @NotNull DataNode<ModuleData> ideModule,
                                           @NotNull DataNode<ProjectData> ideProject);

  @NotNull
  Set<Class> getExtraProjectModelClasses();

  @NotNull
  ProjectImportExtraModelProvider getExtraModelProvider();

  /**
   * add paths containing these classes to classpath of gradle tooling extension
   *
   * @return classes to be available for gradle
   */
  @NotNull
  Set<Class> getToolingExtensionsClasses();

  /**
   * add target types to be used in the polymorphic containers
   * @return
   */
  default Set<Class> getTargetTypes() {
    return Collections.emptySet();
  }

  @NotNull
  List<Pair<String, String>> getExtraJvmArgs();

  @NotNull
  List<String> getExtraCommandLineArgs();

  @NotNull
  ExternalSystemException getUserFriendlyError(@NotNull Throwable error, @NotNull String projectPath, @Nullable String buildFilePath);

  /**
   * Performs project configuration and other checks before the actual project import (before invocation of gradle tooling API).
   */
  void preImportCheck();

  void enhanceTaskProcessing(@NotNull List<String> taskNames, @Nullable String jvmAgentSetup, @NotNull Consumer<String> initScriptConsumer);
}

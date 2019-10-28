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
import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.IntermediateResultHandler;
import org.gradle.tooling.model.BuildModel;
import org.gradle.tooling.model.ProjectModel;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.GradleManager;
import org.jetbrains.plugins.gradle.model.ModelsHolder;
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider;

import java.util.*;

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

  @ApiStatus.Internal
  ExtensionPointName<GradleProjectResolverExtension> EP_NAME = ExtensionPointName.create("org.jetbrains.plugins.gradle.projectResolve");

  void setProjectResolverContext(@NotNull ProjectResolverContext projectResolverContext);

  void setNext(@NotNull GradleProjectResolverExtension projectResolverExtension);

  @Nullable
  GradleProjectResolverExtension getNext();

  /**
   * @deprecated is not used anymore
   */
  @NotNull
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.2")
  default ProjectData createProject() {
    throw new UnsupportedOperationException();
  }

  void populateProjectExtraModels(@NotNull IdeaProject gradleProject, @NotNull DataNode<ProjectData> ideProject);

  @Nullable
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

  /**
   * Called when the project data has been obtained and resolved
   * @param projectDataNode project data graph
   */
  @ApiStatus.Experimental
  default void resolveFinished(@NotNull DataNode<ProjectData> projectDataNode) {}

  @NotNull
  Set<Class> getExtraProjectModelClasses();

  /**
   * Allows to request gradle tooling models after "sync" tasks are run
   *
   * @see BuildActionExecuter.Builder#buildFinished(org.gradle.tooling.BuildAction, org.gradle.tooling.IntermediateResultHandler)
   */
  @Nullable
  default ProjectImportModelProvider getModelProvider() {return null;}

  /**
   * Allows to request gradle tooling models after gradle projects are loaded and before "sync" tasks are run.
   * This can be used to setup "sync" tasks for the import
   *
   * @see BuildActionExecuter.Builder#projectsLoaded(org.gradle.tooling.BuildAction, org.gradle.tooling.IntermediateResultHandler)
   * @see GradleProjectResolverExtension#requiresTaskRunning()
   */
  @Nullable
  default ProjectImportModelProvider getProjectsLoadedModelProvider() {return null;}

  /**
   * @return whether or not this resolver requires Gradle task running infrastructure to be initialized, if any of the resolvers which are
   * used by the resolution return true then the {@link org.gradle.tooling.BuildActionExecuter} will have
   * {@link org.gradle.tooling.BuildActionExecuter#forTasks(String...)} called with an empty list. This will allow
   * any tasks that are scheduled by Gradle plugin in the model builders to be run.
   *
   * Note: If nothing inside Gradle (i.e the model builders) overwrites the task list then this will cause the default task to be run.
   */
  default boolean requiresTaskRunning() {
    return false;
  }

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
  ExternalSystemException getUserFriendlyError(@Nullable BuildEnvironment buildEnvironment,
                                               @NotNull Throwable error,
                                               @NotNull String projectPath,
                                               @Nullable String buildFilePath);

  /**
   * Performs project configuration and other checks before the actual project import (before invocation of gradle tooling API).
   */
  void preImportCheck();

  /**
   * Called once Gradle has loaded projects but before any tasks execution.
   * These models do not contain those models which is created when build finished.
   * <p>
   * Note: This method is called from a Gradle connection thread, within the {@link IntermediateResultHandler} passed to the
   * tooling api.
   *
   * @param models obtained after projects loaded phase
   * @see #getProjectsLoadedModelProvider()
   */
  default void projectsLoaded(@Nullable ModelsHolder<BuildModel, ProjectModel> models) {}

  /**
   * Called once Gradle has finished executing everything, including any tasks that might need to be run. The models are obtained
   * separately and in some cases before this method is called.
   *
   * @param exception the exception thrown by Gradle, if everything completes successfully then this will be null.
   *
   * Note: This method is called from a Gradle connection thread, within the {@link org.gradle.tooling.ResultHandler} passed to the
   * tooling api.
   */
  default void buildFinished(@Nullable GradleConnectionException exception) { }

  /**
   * Allows extension to contribute to init script
   * @param taskNames gradle task names to be executed
   * @param jvmParametersSetup jvm configuration that will be applied to Gradle jvm
   * @param initScriptConsumer consumer of init script text. Must be called to add script txt
   */
  void enhanceTaskProcessing(@NotNull List<String> taskNames, @Nullable String jvmParametersSetup, @NotNull Consumer<String> initScriptConsumer);

  // jvm configuration that will be applied to Gradle jvm
  String JVM_PARAMETERS_SETUP_KEY = "JVM_PARAMETERS_SETUP";

  // flag that shows if tasks will be treated as tests invocation by the IDE (e.g., test events are expected)
  String TEST_EXECUTION_EXPECTED_KEY = "TEST_EXECUTION_EXPECTED";

  // port for callbacks which Gradle tasks communicate to IDE
  String DEBUG_DISPATCH_PORT_KEY = "DEBUG_DISPATCH_PORT";

  // options passed from project to Gradle
  String DEBUG_OPTIONS_KEY = "DEBUG_OPTIONS";

  /**
   * Allows extension to contribute to init script
   * @param taskNames gradle task names to be executed
   * @param jvmParametersSetup jvm configuration that will be applied to Gradle jvm
   * @param initScriptConsumer consumer of init script text. Must be called to add script txt
   * @param parameters storage for passing optional named parameters
   */
  @ApiStatus.Experimental
  default void enhanceTaskProcessing(@NotNull List<String> taskNames,
                                     @NotNull Consumer<String> initScriptConsumer,
                                     @NotNull Map<String, String> parameters) {
    String jvmParametersSetup = parameters.get(JVM_PARAMETERS_SETUP_KEY);
    enhanceTaskProcessing(taskNames, jvmParametersSetup, initScriptConsumer);
  }
}

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
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.UserDataHolderEx;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ProjectImportAction;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;

import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public interface ProjectResolverContext extends UserDataHolderEx {
  @NotNull
  ExternalSystemTaskId getExternalSystemTaskId();

  @Nullable
  String getIdeProjectPath();

  @NotNull
  String getProjectPath();

  @Nullable
  GradleExecutionSettings getSettings();

  @NotNull
  ProjectConnection getConnection();

  @Nullable
  CancellationTokenSource getCancellationTokenSource();

  @NotNull
  ExternalSystemTaskNotificationListener getListener();

  boolean isPreviewMode();

  boolean isResolveModulePerSourceSet();

  boolean isUseQualifiedModuleNames();

  default boolean isDelegatedBuild() { return true; }

  @NotNull
  ProjectImportAction.AllModels getModels();

  void setModels(@NotNull ProjectImportAction.AllModels models) ;

  @Nullable
  <T> T getExtraProject(Class<T> modelClazz);

  @Nullable
  <T> T getExtraProject(@Nullable IdeaModule module, Class<T> modelClazz);

  @NotNull
  Collection<String> findModulesWithModel(@NotNull Class modelClazz);

  boolean hasModulesWithModel(@NotNull Class modelClazz);

  void checkCancelled() throws ProcessCanceledException;

  String getProjectGradleVersion();

  @Nullable
  String getBuildSrcGroup();
}

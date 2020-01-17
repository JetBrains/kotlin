// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.build.events.MessageEvent;
import com.intellij.build.issue.BuildIssue;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.UserDataHolderEx;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaModule;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ProjectImportAction;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;

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

  boolean hasModulesWithModel(@NotNull Class modelClazz);

  void checkCancelled() throws ProcessCanceledException;

  String getProjectGradleVersion();

  @Nullable
  String getBuildSrcGroup();

  @ApiStatus.Experimental
  void report(@NotNull MessageEvent.Kind kind, @NotNull BuildIssue buildIssue);
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.build.events.MessageEvent;
import com.intellij.build.events.impl.BuildIssueEventImpl;
import com.intellij.build.issue.BuildIssue;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemBuildEvent;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.UserDataHolderBase;
import org.gradle.initialization.BuildLayoutParameters;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.tooling.model.idea.IdeaModule;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ProjectImportAction;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;

import java.io.File;

/**
 * @author Vladislav.Soroka
 */
public class DefaultProjectResolverContext extends UserDataHolderBase implements ProjectResolverContext {
  @NotNull private final ExternalSystemTaskId myExternalSystemTaskId;
  @NotNull private final String myProjectPath;
  @Nullable private final GradleExecutionSettings mySettings;
  @NotNull private final ExternalSystemTaskNotificationListener myListener;
  private final boolean myIsPreviewMode;
  @NotNull private final CancellationTokenSource myCancellationTokenSource;
  private ProjectConnection myConnection;
  @NotNull
  private ProjectImportAction.AllModels myModels;
  private File myGradleUserHome;
  @Nullable private String myProjectGradleVersion;
  @Nullable private String myBuildSrcGroup;
  @Nullable private BuildEnvironment myBuildEnvironment;
  @Nullable private final GradlePartialResolverPolicy myPolicy;

  public DefaultProjectResolverContext(@NotNull final ExternalSystemTaskId externalSystemTaskId,
                                       @NotNull final String projectPath,
                                       @Nullable final GradleExecutionSettings settings,
                                       @NotNull final ExternalSystemTaskNotificationListener listener,
                                       @Nullable GradlePartialResolverPolicy resolverPolicy,
                                       final boolean isPreviewMode) {
    this(externalSystemTaskId, projectPath, settings, null, listener, resolverPolicy, isPreviewMode);
  }


  public DefaultProjectResolverContext(@NotNull final ExternalSystemTaskId externalSystemTaskId,
                                       @NotNull final String projectPath,
                                       @Nullable final GradleExecutionSettings settings,
                                       final ProjectConnection connection,
                                       @NotNull final ExternalSystemTaskNotificationListener listener,
                                       @Nullable GradlePartialResolverPolicy resolverPolicy,
                                       final boolean isPreviewMode) {
    myExternalSystemTaskId = externalSystemTaskId;
    myProjectPath = projectPath;
    mySettings = settings;
    myConnection = connection;
    myListener = listener;
    myPolicy = resolverPolicy;
    myIsPreviewMode = isPreviewMode;
    myCancellationTokenSource = GradleConnector.newCancellationTokenSource();
  }

  @NotNull
  @Override
  public ExternalSystemTaskId getExternalSystemTaskId() {
    return myExternalSystemTaskId;
  }

  @Nullable
  @Override
  public String getIdeProjectPath() {
    return mySettings != null ? mySettings.getIdeProjectPath() : null;
  }

  @NotNull
  @Override
  public String getProjectPath() {
    return myProjectPath;
  }

  @Nullable
  @Override
  public GradleExecutionSettings getSettings() {
    return mySettings;
  }

  @NotNull
  @Override
  public ProjectConnection getConnection() {
    return myConnection;
  }

  public void setConnection(@NotNull ProjectConnection connection) {
    myConnection = connection;
  }

  @NotNull
  @Override
  public CancellationTokenSource getCancellationTokenSource() {
    return myCancellationTokenSource;
  }

  @NotNull
  @Override
  public ExternalSystemTaskNotificationListener getListener() {
    return myListener;
  }

  @Override
  public boolean isPreviewMode() {
    return myIsPreviewMode;
  }

  @Override
  public boolean isResolveModulePerSourceSet() {
    return mySettings == null || mySettings.isResolveModulePerSourceSet();
  }

  @Override
  public boolean isUseQualifiedModuleNames() {
    return mySettings != null && mySettings.isUseQualifiedModuleNames();
  }

  @Override
  public boolean isDelegatedBuild() {
    return mySettings == null || mySettings.isDelegatedBuild();
  }

  public File getGradleUserHome() {
    if (myGradleUserHome == null) {
      String serviceDirectory = mySettings == null ? null : mySettings.getServiceDirectory();
      myGradleUserHome = serviceDirectory != null ? new File(serviceDirectory) : new BuildLayoutParameters().getGradleUserHomeDir();
    }
    return myGradleUserHome;
  }

  @NotNull
  @Override
  public ProjectImportAction.AllModels getModels() {
    return myModels;
  }

  @Override
  public void setModels(@NotNull ProjectImportAction.AllModels models) {
    myModels = models;
  }

  @Nullable
  @Override
  public <T> T getExtraProject(Class<T> modelClazz) {
    return myModels.getModel(modelClazz);
  }

  @Nullable
  @Override
  public <T> T getExtraProject(@Nullable IdeaModule module, Class<T> modelClazz) {
    return module == null ? myModels.getModel(modelClazz) : myModels.getModel(module, modelClazz);
  }

  @Override
  public boolean hasModulesWithModel(@NotNull Class modelClazz) {
    return myModels.hasModulesWithModel(modelClazz);
  }

  @Override
  public void checkCancelled() {
    if (myCancellationTokenSource.token().isCancellationRequested()) {
      throw new ProcessCanceledException();
    }
  }

  @Override
  public String getProjectGradleVersion() {
    if (myProjectGradleVersion == null) {
      if (myBuildEnvironment == null) {
        myBuildEnvironment = getModels().getBuildEnvironment();
      }
      if (myBuildEnvironment != null) {
        myProjectGradleVersion = myBuildEnvironment.getGradle().getGradleVersion();
      }
    }
    return myProjectGradleVersion;
  }

  public void setBuildSrcGroup(@Nullable String groupId) {
    myBuildSrcGroup = groupId;
  }

  @Nullable
  @Override
  public String getBuildSrcGroup() {
    return myBuildSrcGroup;
  }

  @Override
  public void report(@NotNull MessageEvent.Kind kind, @NotNull BuildIssue buildIssue) {
    BuildIssueEventImpl buildIssueEvent = new BuildIssueEventImpl(myExternalSystemTaskId, buildIssue, kind);
    myListener.onStatusChange(new ExternalSystemBuildEvent(myExternalSystemTaskId, buildIssueEvent));
  }

  void setBuildEnvironment(@NotNull BuildEnvironment buildEnvironment) {
    myBuildEnvironment = buildEnvironment;
  }

  @Nullable
  public BuildEnvironment getBuildEnvironment() {
    return myBuildEnvironment;
  }

  @Nullable
  @ApiStatus.Experimental
  public GradlePartialResolverPolicy getPolicy() {
    return myPolicy;
  }
}

/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import com.intellij.openapi.util.UserDataHolderBase;
import org.gradle.initialization.BuildLayoutParameters;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.tooling.model.idea.IdeaModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ProjectImportAction;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;

import java.io.File;
import java.util.Collection;

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

  public DefaultProjectResolverContext(@NotNull final ExternalSystemTaskId externalSystemTaskId,
                                       @NotNull final String projectPath,
                                       @Nullable final GradleExecutionSettings settings,
                                       @NotNull final ExternalSystemTaskNotificationListener listener,
                                       final boolean isPreviewMode) {
    this(externalSystemTaskId, projectPath, settings, null, listener, isPreviewMode);
  }


  public DefaultProjectResolverContext(@NotNull final ExternalSystemTaskId externalSystemTaskId,
                                       @NotNull final String projectPath,
                                       @Nullable final GradleExecutionSettings settings,
                                       final ProjectConnection connection,
                                       @NotNull final ExternalSystemTaskNotificationListener listener,
                                       final boolean isPreviewMode) {
    myExternalSystemTaskId = externalSystemTaskId;
    myProjectPath = projectPath;
    mySettings = settings;
    myConnection = connection;
    myListener = listener;
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
    return myModels.getExtraProject((IdeaModule)null, modelClazz);
  }

  @Nullable
  @Override
  public <T> T getExtraProject(@Nullable IdeaModule module, Class<T> modelClazz) {
    return myModels.getExtraProject(module != null ? module.getGradleProject() : null, modelClazz);
  }

  @NotNull
  @Override
  public Collection<String> findModulesWithModel(@NotNull Class modelClazz) {
    return myModels.findModulesWithModel(modelClazz);
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
      final BuildEnvironment env = getModels().getBuildEnvironment();
      if (env != null) {
        myProjectGradleVersion = env.getGradle().getGradleVersion();
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
}

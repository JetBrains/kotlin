// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import org.gradle.tooling.GradleConnector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.internal.daemon.GradleDaemonServices;

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
@Service
public final class GradleConnectorService implements Disposable {
  private static final Logger LOG = Logger.getInstance(GradleConnectorService.class);
  // todo should be replaced when it will be possible to distinguish active gradle daemon processes used by other clients
  // or removed by using GradleConnector#disconnect() api from https://github.com/gradle/gradle/pull/12687
  private static final boolean DISABLE_DAEMONS_STOP = Boolean.getBoolean("idea.gradle.disableDaemonsStopOnExit");

  public GradleConnectorService(@SuppressWarnings("unused") Project project) { }

  @Override
  public void dispose() {
    if (ApplicationManager.getApplication().isUnitTestMode() || DISABLE_DAEMONS_STOP) return;
    // do not use DefaultGradleConnector.close() it sends org.gradle.launcher.daemon.protocol.StopWhenIdle message and waits
    try {
      // todo this call should be replaced by GradleConnector#disconnect() api from https://github.com/gradle/gradle/pull/12687
      if (ProjectUtil.getOpenProjects().length == 0) {
        GradleDaemonServices.stopDaemons();
      }
    }
    catch (Exception e) {
      LOG.warn("Failed to stop Gradle daemons during IDE shutdown", e);
    }
  }

  public GradleConnector getConnector() {
    // Currently GradleConnector is not thread-safe as stated in the javadoc
    // We need to check if it can be (reset and) reused after https://github.com/gradle/gradle/pull/12687
    // or we need to save all instances for each TAPI request for further cleanup
    return GradleConnector.newConnector();
  }

  public static GradleConnector getConnector(@NotNull String projectPath, @Nullable ExternalSystemTaskId taskId) {
    Project project = taskId != null ? taskId.findProject() : null;
    if (project == null) {
      for (Project openProject : ProjectUtil.getOpenProjects()) {
        String projectBasePath = openProject.getBasePath();
        if (projectBasePath == null) continue;
        if (FileUtil.isAncestor(projectBasePath, projectPath, false)) {
          project = openProject;
          break;
        }
      }
    }
    return project == null ? GradleConnector.newConnector() : project.getService(GradleConnectorService.class).getConnector();
  }
}

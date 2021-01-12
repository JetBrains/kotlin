// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.open

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode.MODAL_SYNC
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.service.ui.ExternalProjectDataSelectorDialog
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.startup.GradleUnlinkedProjectProcessor
import org.jetbrains.plugins.gradle.util.GradleConstants.BUILD_FILE_EXTENSIONS
import org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID
import org.jetbrains.plugins.gradle.util.setupGradleJvm
import org.jetbrains.plugins.gradle.util.updateGradleJvm
import org.jetbrains.plugins.gradle.util.validateJavaHome
import java.nio.file.Path

internal class GradleOpenProjectProvider : AbstractOpenProjectProvider() {
  override fun isProjectFile(file: VirtualFile): Boolean {
    return !file.isDirectory && BUILD_FILE_EXTENSIONS.any { file.name.endsWith(it) }
  }

  override fun linkAndRefreshProject(projectDirectory: Path, project: Project) {
    val gradleSettings = GradleSettings.getInstance(project)
    gradleSettings.setupGradleSettings()
    val gradleProjectSettings = GradleProjectSettings()
    gradleProjectSettings.setupGradleProjectSettings(projectDirectory)

    val gradleVersion = gradleProjectSettings.resolveGradleVersion()
    setupGradleJvm(project, gradleProjectSettings, gradleVersion)

    attachGradleProjectAndRefresh(gradleProjectSettings, project)

    validateJavaHome(project, projectDirectory, gradleVersion)
  }

  override fun openProject(projectFile: VirtualFile, projectToClose: Project?, forceOpenInNewFrame: Boolean): Project? {
    val project = super.openProject(projectFile, projectToClose, forceOpenInNewFrame)
    if (project != null) {
      GradleUnlinkedProjectProcessor.enableNotifications(project)
    }
    return project
  }

  private fun attachGradleProjectAndRefresh(settings: ExternalProjectSettings, project: Project) {
    val externalProjectPath = settings.externalProjectPath
    ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized {
      ExternalSystemUtil.ensureToolWindowInitialized(project, SYSTEM_ID)
    }
    ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).linkProject(settings)
    ExternalSystemUtil.refreshProject(externalProjectPath,
                                      ImportSpecBuilder(project, SYSTEM_ID)
                                        .usePreviewMode()
                                        .use(MODAL_SYNC))
    ExternalSystemUtil.refreshProject(externalProjectPath,
                                      ImportSpecBuilder(project, SYSTEM_ID)
                                        .callback(createFinalImportCallback(project, externalProjectPath)))
  }

  private fun createFinalImportCallback(project: Project, externalProjectPath: String): ExternalProjectRefreshCallback {
    return object : ExternalProjectRefreshCallback {
      override fun onSuccess(externalProject: DataNode<ProjectData>?) {
        if (externalProject == null) return
        selectDataToImport(project, externalProjectPath, externalProject)
        importData(project, externalProject)
        updateGradleJvm(project, externalProjectPath)
      }
    }
  }

  private fun selectDataToImport(project: Project, externalProjectPath: String, externalProject: DataNode<ProjectData>) {
    val settings = GradleSettings.getInstance(project)
    val showSelectiveImportDialog = settings.showSelectiveImportDialogOnInitialImport()
    val application = ApplicationManager.getApplication()
    if (showSelectiveImportDialog && !application.isHeadlessEnvironment) {
      application.invokeAndWait {
        val projectInfo = InternalExternalProjectInfo(SYSTEM_ID, externalProjectPath, externalProject)
        val dialog = ExternalProjectDataSelectorDialog(project, projectInfo)
        if (dialog.hasMultipleDataToSelect()) {
          dialog.showAndGet()
        }
        else {
          Disposer.dispose(dialog.disposable)
        }
      }
    }
  }

  private fun importData(project: Project, externalProject: DataNode<ProjectData>) {
    ProjectDataManager.getInstance().importData(externalProject, project, false)
  }
}
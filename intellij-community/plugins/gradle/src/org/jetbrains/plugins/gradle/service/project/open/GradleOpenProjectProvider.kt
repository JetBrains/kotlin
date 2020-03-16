// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.open

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
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
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.text.nullize
import org.jetbrains.plugins.gradle.service.GradleInstallationManager
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.startup.GradleUnlinkedProjectProcessor
import org.jetbrains.plugins.gradle.util.*
import org.jetbrains.plugins.gradle.util.GradleConstants.BUILD_FILE_EXTENSIONS
import org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID

internal class GradleOpenProjectProvider : AbstractOpenProjectProvider() {
  override fun isProjectFile(file: VirtualFile): Boolean {
    return !file.isDirectory && BUILD_FILE_EXTENSIONS.any { file.name.endsWith(it) }
  }

  override fun linkAndRefreshProject(projectDirectory: String, project: Project) {
    val projectSdk = ProjectRootManager.getInstance(project).projectSdk
    val gradleProjectSettings = GradleProjectSettings()
    setupGradleSettings(gradleProjectSettings, projectDirectory, project, projectSdk)
    attachGradleProjectAndRefresh(gradleProjectSettings, project)
    validateJavaHome(project, projectDirectory, gradleProjectSettings.resolveGradleVersion())
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

  fun setupGradleSettings(settings: GradleProjectSettings, projectDirectory: String, project: Project, projectSdk: Sdk? = null) {
    GradleSettings.getInstance(project).setupGradleSettings()
    settings.setupGradleProjectSettings(projectDirectory, project, projectSdk)
  }

  private fun GradleSettings.setupGradleSettings() {
    gradleVmOptions = GradleEnvironment.Headless.GRADLE_VM_OPTIONS ?: gradleVmOptions
    isOfflineWork = GradleEnvironment.Headless.GRADLE_OFFLINE?.toBoolean() ?: isOfflineWork
    serviceDirectoryPath = GradleEnvironment.Headless.GRADLE_SERVICE_DIRECTORY ?: serviceDirectoryPath
    storeProjectFilesExternally = true
  }

  private fun GradleProjectSettings.setupGradleProjectSettings(projectDirectory: String, project: Project, projectSdk: Sdk? = null) {
    externalProjectPath = projectDirectory
    isUseQualifiedModuleNames = true
    distributionType = GradleEnvironment.Headless.GRADLE_DISTRIBUTION_TYPE?.let(DistributionType::valueOf)
                       ?: DistributionType.DEFAULT_WRAPPED
    gradleHome = GradleEnvironment.Headless.GRADLE_HOME ?: suggestGradleHome()
    gradleJvm = suggestGradleJvm(project, projectSdk, projectDirectory, resolveGradleVersion())
  }

  private fun suggestGradleHome(): String? {
    val installationManager = ServiceManager.getService(GradleInstallationManager::class.java)
    val lastUsedGradleHome = GradleUtil.getLastUsedGradleHome().nullize()
    if (lastUsedGradleHome != null) return lastUsedGradleHome
    val gradleHome = installationManager.autodetectedGradleHome ?: return null
    return FileUtil.toCanonicalPath(gradleHome.path)
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
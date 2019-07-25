// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("GradleProjectImportUtil")
package org.jetbrains.plugins.gradle.service.project.open

import com.intellij.ide.GeneralSettings
import com.intellij.ide.highlighter.ProjectFileType
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode.IN_BACKGROUND_ASYNC
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode.MODAL_SYNC
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SimpleJavaSdkType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.util.EnvironmentUtil
import com.intellij.util.io.exists
import com.intellij.util.text.nullize
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.service.GradleInstallationManager
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.DistributionType.DEFAULT_WRAPPED
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.startup.GradleUnlinkedProjectProcessor
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID
import org.jetbrains.plugins.gradle.util.GradleEnvironment.Headless.*
import org.jetbrains.plugins.gradle.util.GradleLog.LOG
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.nio.file.InvalidPathException
import java.nio.file.Paths

private fun isGradleProjectFile(file: VirtualFile): Boolean {
  return !file.isDirectory && GradleConstants.BUILD_FILE_EXTENSIONS.any { file.name.endsWith(it) }
}

fun canImportProjectFrom(file: VirtualFile): Boolean {
  if (!file.isDirectory) return isGradleProjectFile(file)
  return file.children.any(::isGradleProjectFile)
}

@ApiStatus.Experimental
fun openProject(projectFile: VirtualFile, projectToClose: Project?, forceOpenInNewFrame: Boolean): Project? {
  if (!canImportProjectFrom(projectFile)) return null
  val projectDirectory = findExternalProjectDirectory(projectFile)
  if (focusOnOpenedSameProject(projectDirectory.path)) return null
  if (canOpenPlatformProject(projectDirectory)) {
    return openPlatformProject(projectDirectory, projectToClose, forceOpenInNewFrame)
  }
  val project = createProject(projectDirectory) ?: return null
  importProject(projectDirectory.path, project)
  if (!forceOpenInNewFrame) closePreviousProject(projectToClose)
  ProjectManagerEx.getInstanceEx().openProject(project)
  return project
}

@ApiStatus.Experimental
fun importProject(projectDirectory: String, project: Project) {
  LOG.info("Import project at $projectDirectory")
  val projectSdk = ProjectRootManager.getInstance(project).projectSdk
  val gradleProjectSettings = GradleProjectSettings()
  setupGradleSettings(gradleProjectSettings, projectDirectory, project, projectSdk)
  attachGradleProjectAndRefresh(gradleProjectSettings, project)
  ProjectUtil.updateLastProjectLocation(projectDirectory)
  project.save()
}

private fun canOpenPlatformProject(projectDirectory: VirtualFile): Boolean {
  if (!PlatformProjectOpenProcessor.getInstance().canOpenProject(projectDirectory)) return false
  if (isChildExistsUsingIo(projectDirectory, Project.DIRECTORY_STORE_FOLDER)) return true
  if (isChildExistsUsingIo(projectDirectory, projectDirectory.name + ProjectFileType.DOT_DEFAULT_EXTENSION)) return true
  return false
}

private fun isChildExistsUsingIo(parent: VirtualFile, name: String): Boolean {
  return try {
    Paths.get(FileUtil.toSystemDependentName(parent.path), name).exists()
  }
  catch (e: InvalidPathException) {
    false
  }
}

private fun focusOnOpenedSameProject(projectDirectory: String): Boolean {
  for (project in ProjectManager.getInstance().openProjects) {
    if (ProjectUtil.isSameProject(projectDirectory, project)) {
      ProjectUtil.focusProjectWindow(project, false)
      return true
    }
  }
  return false
}

private fun openPlatformProject(projectDirectory: VirtualFile, projectToClose: Project?, forceOpenInNewFrame: Boolean): Project? {
  val openProcessor = PlatformProjectOpenProcessor.getInstance()
  return openProcessor.doOpenProject(projectDirectory, projectToClose, forceOpenInNewFrame)?.also {
    GradleUnlinkedProjectProcessor.enableNotifications(it)
  }
}

fun attachGradleProjectAndRefresh(settings: ExternalProjectSettings, project: Project) {
  val externalProjectPath = settings.externalProjectPath
  ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized {
    DumbService.getInstance(project).runWhenSmart {
      ExternalSystemUtil.ensureToolWindowInitialized(project, SYSTEM_ID)
    }
  }
  ExternalProjectsManagerImpl.disableProjectWatcherAutoUpdate(project)
  ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).linkProject(settings)
  ExternalSystemUtil.refreshProject(project, SYSTEM_ID, externalProjectPath, true, MODAL_SYNC)
  ExternalSystemUtil.refreshProject(project, SYSTEM_ID, externalProjectPath, false, IN_BACKGROUND_ASYNC)
}

private fun closePreviousProject(projectToClose: Project?) {
  val openProjects = ProjectManager.getInstance().openProjects
  if (openProjects.isNotEmpty()) {
    val exitCode = ProjectUtil.confirmOpenNewProject(true)
    if (exitCode == GeneralSettings.OPEN_PROJECT_SAME_WINDOW) {
      ProjectUtil.closeAndDispose(projectToClose ?: openProjects[openProjects.size - 1])
    }
  }
}

private fun findExternalProjectDirectory(file: VirtualFile): VirtualFile {
  if (!file.isDirectory) return file.parent
  return file
}

private fun createProject(projectDirectory: VirtualFile): Project? {
  val projectManager = ProjectManagerEx.getInstanceEx()
  val project = projectManager.createProject(projectDirectory.name, projectDirectory.path)
  project?.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, true)
  return project
}

fun setupGradleSettings(settings: GradleProjectSettings, projectDirectory: String, project: Project, projectSdk: Sdk? = null) {
  GradleSettings.getInstance(project).setupGradleSettings()
  settings.setupGradleProjectSettings(projectDirectory, project, projectSdk)
}

private fun GradleSettings.setupGradleSettings() {
  gradleVmOptions = GRADLE_VM_OPTIONS ?: gradleVmOptions
  isOfflineWork = GRADLE_OFFLINE?.toBoolean() ?: isOfflineWork
  serviceDirectoryPath = GRADLE_SERVICE_DIRECTORY ?: serviceDirectoryPath
  if (ExternalSystemUtil.isNewProject(project) && linkedProjectsSettings.isEmpty()) {
    storeProjectFilesExternally = true
  }
}

private fun GradleProjectSettings.setupGradleProjectSettings(projectDirectory: String, project: Project, projectSdk: Sdk? = null) {
  externalProjectPath = projectDirectory
  isUseAutoImport = false
  isUseQualifiedModuleNames = true
  distributionType = GRADLE_DISTRIBUTION_TYPE?.let(DistributionType::valueOf) ?: DEFAULT_WRAPPED
  gradleHome = GRADLE_HOME ?: suggestGradleHome()
  gradleJvm = suggestGradleJvm(project, projectSdk, resolveGradleVersion())
}

private fun suggestGradleHome(): String? {
  val installationManager = ServiceManager.getService(GradleInstallationManager::class.java)
  val lastUsedGradleHome = GradleUtil.getLastUsedGradleHome().nullize()
  if (lastUsedGradleHome != null) return lastUsedGradleHome
  val gradleHome = installationManager.autodetectedGradleHome ?: return null
  return FileUtil.toCanonicalPath(gradleHome.path)
}

private fun suggestGradleJvm(project: Project, projectSdk: Sdk?, gradleVersion: GradleVersion): String? {
  with(SettingsContext(project, projectSdk, gradleVersion)) {
    return getGradleJdkReference()
           ?: getProjectJdkReference()
           ?: getMostRecentJdkReference()
           ?: getJavaHomeJdkReference()
           ?: getAndAddExternalJdkReference()
  }
}

private class SettingsContext(val project: Project, val projectSdk: Sdk?, val gradleVersion: GradleVersion)

private fun SettingsContext.getGradleJdkReference(): String? {
  val settings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
  return settings.getLinkedProjectsSettings()
    .filterIsInstance<GradleProjectSettings>()
    .mapNotNull { it.gradleJvm }
    .firstOrNull()
}

private fun SettingsContext.getJavaHomeJdkReference(): String? {
  val javaHome = EnvironmentUtil.getEnvironmentMap()["JAVA_HOME"] ?: return null
  val jdk = GradleJdk.valueOf(javaHome) ?: return null
  if (!jdk.isSupported(gradleVersion)) return null
  val simpleJavaSdkType = SimpleJavaSdkType.getInstance()
  val sdkName = simpleJavaSdkType.suggestSdkName(null, javaHome)
  simpleJavaSdkType.createJdk(sdkName, javaHome)
  return ExternalSystemJdkUtil.USE_JAVA_HOME
}

private fun SettingsContext.getProjectJdkReference(): String? {
  val projectSdk = projectSdk ?: ProjectRootManager.getInstance(project).projectSdk
  val projectJdk = projectSdk?.let(GradleJdk.Companion::valueOf) ?: return null
  if (!projectJdk.isSupported(gradleVersion)) return null
  return ExternalSystemJdkUtil.USE_PROJECT_JDK
}

private fun SettingsContext.getMostRecentJdkReference(): String? {
  val projectJdkTable = ProjectJdkTable.getInstance()
  val javaSdkType = ExternalSystemJdkUtil.getJavaSdkType()
  val jdk = projectJdkTable.getSdksOfType(javaSdkType)
    .mapNotNull { GradleJdk.valueOf(it) }
    .filter { it.isSupported(gradleVersion) }
    .maxBy { it.version }
  return jdk?.name
}

private fun SettingsContext.getAndAddExternalJdkReference(): String? {
  val jdk = ExternalSystemJdkUtil.suggestJdkHomePaths()
    .mapNotNull { GradleJdk.valueOf(it) }
    .filter { it.isSupported(gradleVersion) }
    .maxBy { it.version }
  if (jdk == null) return null
  return ExternalSystemJdkUtil.addJdk(jdk.homePath).name
}

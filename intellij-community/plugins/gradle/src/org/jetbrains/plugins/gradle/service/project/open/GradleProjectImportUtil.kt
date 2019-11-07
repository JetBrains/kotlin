// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("GradleProjectImportUtil")
package org.jetbrains.plugins.gradle.service.project.open

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

fun canOpenGradleProject(file: VirtualFile): Boolean =
  GradleOpenProjectProvider().canOpenProject(file)

fun openGradleProject(projectFile: VirtualFile, projectToClose: Project?, forceOpenInNewFrame: Boolean): Project? =
  GradleOpenProjectProvider().openProject(projectFile, projectToClose, forceOpenInNewFrame)

@ApiStatus.Experimental
@JvmOverloads
fun canLinkAndRefreshGradleProject(projectFilePath: String, project: Project, showValidationDialog: Boolean = true): Boolean {
  val validationInfo = validateGradleProject(projectFilePath, project) ?: return true
  if (showValidationDialog) {
    val title = ExternalSystemBundle.message("error.project.import.error.title")
    when (validationInfo.warning) {
      true -> Messages.showWarningDialog(project, validationInfo.message, title)
      else -> Messages.showErrorDialog(project, validationInfo.message, title)
    }
  }
  return false
}

fun linkAndRefreshGradleProject(projectFilePath: String, project: Project) {
  val localFileSystem = LocalFileSystem.getInstance()
  val projectFile = localFileSystem.refreshAndFindFileByPath(projectFilePath)
  if (projectFile == null) throw IllegalArgumentException(ExternalSystemBundle.message("error.project.does.not.exist"))
  GradleOpenProjectProvider().linkToExistingProject(projectFile, project)
}

fun setupGradleSettings(settings: GradleProjectSettings, projectDirectory: String, project: Project, projectSdk: Sdk? = null) =
  GradleOpenProjectProvider().setupGradleSettings(settings, projectDirectory, project, projectSdk)

private fun validateGradleProject(projectFilePath: String, project: Project): ValidationInfo? {
  val systemSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
  val localFileSystem = LocalFileSystem.getInstance()
  val projectFile = localFileSystem.refreshAndFindFileByPath(projectFilePath)
  if (projectFile == null) return ValidationInfo(ExternalSystemBundle.message("error.project.does.not.exist"))
  val projectDirectory = if (projectFile.isDirectory) projectFile else projectFile.parent
  val projectSettings = systemSettings.getLinkedProjectSettings(projectDirectory.path)
  if (projectSettings != null) return ValidationInfo(ExternalSystemBundle.message("error.project.already.registered"))
  return null
}
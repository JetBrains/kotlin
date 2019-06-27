// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("GradleProjectImportUtil")
package org.jetbrains.plugins.gradle.service.project.open

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings

@ApiStatus.Experimental
fun canSetupGradleProjectFrom(file: VirtualFile): Boolean =
  GradleExternalSystemImportProvider().canSetupProjectFrom(file)

@ApiStatus.Experimental
fun openGradleProject(projectFile: VirtualFile, projectToClose: Project?, forceOpenInNewFrame: Boolean): Project? =
  GradleExternalSystemImportProvider().openProject(projectFile, projectToClose, forceOpenInNewFrame)

@ApiStatus.Experimental
fun linkAndRefreshGradleProject(projectFilePath: String, project: Project) {
  val localFileSystem = LocalFileSystem.getInstance()
  val projectFile = localFileSystem.refreshAndFindFileByPath(projectFilePath) ?: return
  GradleExternalSystemImportProvider().linkAndRefreshProject(projectFile, project)
}

@ApiStatus.Experimental
fun setupGradleSettings(settings: GradleProjectSettings, projectDirectory: String, project: Project, projectSdk: Sdk? = null) =
  GradleExternalSystemImportProvider().setupGradleSettings(settings, projectDirectory, project, projectSdk)

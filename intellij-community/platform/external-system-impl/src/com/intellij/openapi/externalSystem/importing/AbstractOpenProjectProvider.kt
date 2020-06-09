// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.importing

import com.intellij.ide.highlighter.ProjectFileType
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.util.io.exists
import org.jetbrains.annotations.ApiStatus
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

@ApiStatus.Experimental
abstract class AbstractOpenProjectProvider : OpenProjectProvider {
  protected abstract fun isProjectFile(file: VirtualFile): Boolean

  protected abstract fun linkAndRefreshProject(projectDirectory: Path, project: Project)

  override fun canOpenProject(file: VirtualFile): Boolean {
    return if (file.isDirectory) file.children.any(::isProjectFile) else isProjectFile(file)
  }

  override fun openProject(projectFile: VirtualFile, projectToClose: Project?, forceOpenInNewFrame: Boolean): Project? {
    LOG.debug("Open project from $projectFile")
    val projectDirectory = getProjectDirectory(projectFile)
    if (focusOnOpenedSameProject(projectDirectory.path)) {
      return null
    }
    else if (canOpenPlatformProject(projectDirectory)) {
      return openPlatformProject(projectDirectory, projectToClose, forceOpenInNewFrame)
    }

    val nioPath = projectDirectory.toNioPath()
    val options = OpenProjectTask(isNewProject = true,
                                  forceOpenInNewFrame = forceOpenInNewFrame,
                                  projectToClose = projectToClose,
                                  runConfigurators = false,
                                  beforeOpen = { project ->
                                    project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, true)
                                    ApplicationManager.getApplication().invokeAndWait {
                                      linkAndRefreshProject(nioPath, project)
                                    }
                                    updateLastProjectLocation(nioPath)
                                    true
                                  })
    return ProjectManagerEx.getInstanceEx().openProject(nioPath, options)
  }

  override fun linkToExistingProject(projectFile: VirtualFile, project: Project) {
    LOG.debug("Import project from $projectFile")
    val projectDirectory = getProjectDirectory(projectFile)
    linkAndRefreshProject(projectDirectory.toNioPath(), project)
  }

  private fun canOpenPlatformProject(projectDirectory: VirtualFile): Boolean {
    if (!PlatformProjectOpenProcessor.getInstance().canOpenProject(projectDirectory)) {
      return false
    }
    if (isChildExistUsingIo(projectDirectory, Project.DIRECTORY_STORE_FOLDER)) {
      return true
    }
    if (isChildExistUsingIo(projectDirectory, projectDirectory.name + ProjectFileType.DOT_DEFAULT_EXTENSION)) {
      return true
    }
    return false
  }

  private fun isChildExistUsingIo(parent: VirtualFile, name: String): Boolean {
    return try {
      Paths.get(FileUtil.toSystemDependentName(parent.path), name).exists()
    }
    catch (e: InvalidPathException) {
      false
    }
  }

  private fun focusOnOpenedSameProject(projectDirectory: String): Boolean {
    for (project in ProjectManager.getInstance().openProjects) {
      if (isSameProject(projectDirectory, project)) {
        focusProjectWindow(project, false)
        return true
      }
    }
    return false
  }

  private fun openPlatformProject(projectDirectory: VirtualFile, projectToClose: Project?, forceOpenInNewFrame: Boolean): Project? {
    val openProcessor = PlatformProjectOpenProcessor.getInstance()
    return openProcessor.doOpenProject(projectDirectory, projectToClose, forceOpenInNewFrame)
  }

  private fun getProjectDirectory(file: VirtualFile): VirtualFile {
    return if (file.isDirectory) file else file.parent
  }

  companion object {
    protected val LOG = logger<AbstractOpenProjectProvider>()
  }
}
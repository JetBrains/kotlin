/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.platform

import com.intellij.CommonBundle
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.impl.ModuleManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.modifyModules
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsDirectoryMapping
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ModuleAttachProcessor.Companion.getPrimaryModule
import com.intellij.projectImport.ProjectAttachProcessor
import com.intellij.projectImport.ProjectOpenedCallback
import com.intellij.util.io.directoryStreamIfExists
import com.intellij.util.io.exists
import com.intellij.util.io.systemIndependentPath
import java.io.File
import java.nio.file.Path
import java.util.*

private val LOG = Logger.getInstance(ModuleAttachProcessor::class.java)

class ModuleAttachProcessor : ProjectAttachProcessor() {
  companion object {
    @JvmStatic
    fun findModuleInBaseDir(project: Project): Module? {
      val baseDir = project.baseDir
      return ModuleManager.getInstance(project).modules.firstOrNull { it.rootManager.contentRoots.any { it == baseDir } }
    }

    @JvmStatic
    fun getPrimaryModule(project: Project): Module? = if (ProjectAttachProcessor.canAttachToProject()) findModuleInBaseDir(project) else null

    @JvmStatic
    fun getSortedModules(project: Project): List<Module> {
      val primaryModule = getPrimaryModule(project)
      val result = ArrayList<Module>()
      ModuleManager.getInstance(project).modules.filterTo(result) { it !== primaryModule}
      result.sortBy(Module::getName)
      primaryModule?.let {
        result.add(0, it)
      }
      return result
    }

    /**
     * @param project the project
     * @return null if either multi-projects are not enabled or the project has only one module
     */
    @JvmStatic
    fun getMultiProjectDisplayName(project: Project): String? {
      if (!ProjectAttachProcessor.canAttachToProject()) {
        return null
      }

      val modules = ModuleManager.getInstance(project).modules
      if (modules.size <= 1) {
        return null
      }

      val primaryModule = getPrimaryModule(project) ?: modules.first()
      val result = StringBuilder(primaryModule.name)
      result.append(", ")
      for (module in modules) {
        if (module === primaryModule) {
          continue
        }
        result.append(module.name)
        break
      }
      if (modules.size > 2) {
        result.append("...")
      }
      return result.toString()
    }
  }

  override fun attachToProject(project: Project, projectDir: Path, callback: ProjectOpenedCallback?): Boolean {
    val dotIdeaDir = projectDir.resolve(Project.DIRECTORY_STORE_FOLDER)
    if (!dotIdeaDir.exists()) {
      val newProject = ProjectManagerEx.getInstanceEx().newProject(projectDir.fileName.toString(), projectDir.toString(), true, false) ?: return false
      val baseDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(projectDir.systemIndependentPath)
      PlatformProjectOpenProcessor.runDirectoryProjectConfigurators(baseDir, newProject)
      newProject.save()
      runWriteAction { Disposer.dispose(newProject) }
    }

    var isAttached = findMainModule(project, dotIdeaDir, callback)
    if (!isAttached) {
      isAttached = findMainModule(project, projectDir, callback)
    }
    if (isAttached) {
      return true
    }

    return Messages.showYesNoDialog(project,
      "The project at $projectDir uses a non-standard layout and cannot be attached to this project. Would you like to open it in a new window?",
      "Open Project", Messages.getQuestionIcon()) != Messages.YES
  }
}

private fun findMainModule(project: Project, projectDir: Path, callback: ProjectOpenedCallback?): Boolean {
  projectDir.directoryStreamIfExists({ path -> path.fileName.toString().endsWith(ModuleManagerImpl.IML_EXTENSION) }) {
    for (file in it) {
      LocalFileSystem.getInstance().refreshAndFindFileByPath(file.systemIndependentPath)?.let {
        attachModule(project, it, callback)
        return true
      }
    }
  }
  return false
}

private fun attachModule(project: Project, imlFile: VirtualFile, callback: ProjectOpenedCallback?) {
  try {
    val module = project.modifyModules {
      loadModule(imlFile.path)
    }
    val newModule = ModuleManager.getInstance(project).findModuleByName(module.name)!!
    val primaryModule = addPrimaryModuleDependency(project, newModule)
    if (primaryModule != null) {
      val dotIdeaDir = imlFile.parent
      if (dotIdeaDir != null) {
        updateVcsMapping(primaryModule, dotIdeaDir.parent)
      }
    }

    callback?.projectOpened(project, newModule)
  }
  catch (e: Exception) {
    LOG.info(e)
    Messages.showErrorDialog(project, "Cannot attach project: ${e.message}", CommonBundle.getErrorTitle())
  }
}

private fun updateVcsMapping(primaryModule: Module, addedModuleContentRoot: VirtualFile) {
  val project = primaryModule.project
  val vcsManager = ProjectLevelVcsManager.getInstance(project)
  val mappings = vcsManager.directoryMappings
  if (mappings.size == 1) {
    val contentRoots = ModuleRootManager.getInstance(primaryModule).contentRoots
    // if we had one mapping for the root of the primary module and the added module uses the same VCS, change mapping to <Project Root>
    if (contentRoots.size == 1 && FileUtil.filesEqual(File(contentRoots[0].path), File(mappings[0].directory))) {
      val vcs = vcsManager.findVersioningVcs(addedModuleContentRoot)
      if (vcs != null && vcs.name == mappings[0].vcs) {
        vcsManager.directoryMappings = listOf(VcsDirectoryMapping("", vcs.name))
        return
      }
    }
  }
  val vcs = vcsManager.findVersioningVcs(addedModuleContentRoot)
  if (vcs != null) {
    val newMappings = ArrayList(mappings)
    newMappings.add(VcsDirectoryMapping(addedModuleContentRoot.path, vcs.name))
    vcsManager.directoryMappings = newMappings
  }
}

private fun addPrimaryModuleDependency(project: Project, newModule: Module): Module? {
  val module = getPrimaryModule(project)
  if (module != null && module !== newModule) {
    ModuleRootModificationUtil.addDependency(module, newModule)
    return module
  }
  return null
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.ide.highlighter.ProjectFileType
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.components.impl.stores.IComponentStore
import com.intellij.openapi.components.impl.stores.IProjectStore
import com.intellij.openapi.diagnostic.runAndLogException
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.project.ex.ProjectNameProvider
import com.intellij.openapi.project.impl.ProjectImpl
import com.intellij.openapi.project.impl.ProjectStoreFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtilRt
import com.intellij.util.SmartList
import com.intellij.util.containers.computeIfAny
import com.intellij.util.io.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.CalledInAny
import java.nio.file.AccessDeniedException
import java.nio.file.Path
import java.nio.file.Paths

internal val IProjectStore.nameFile: Path
  get() = Paths.get(directoryStorePath, ProjectImpl.NAME_FILE)

@ApiStatus.Internal
open class ProjectStoreImpl(project: Project) : ProjectStoreBase(project) {
  private var lastSavedProjectName: String? = null

  init {
    assert(!project.isDefault)
  }

  final override fun getPathMacroManagerForDefaults() = PathMacroManager.getInstance(project)

  override val storageManager = ProjectStateStorageManager(TrackingPathMacroSubstitutorImpl(PathMacroManager.getInstance(project)), project)

  override fun setPath(path: String) {
    setPath(path, true, null)
  }

  override fun getProjectName(): String {
    if (!isDirectoryBased) {
      return PathUtilRt.getFileName(projectFilePath).removeSuffix(ProjectFileType.DOT_DEFAULT_EXTENSION)
    }

    val baseDir = projectBasePath
    val nameFile = nameFile
    if (nameFile.exists()) {
      LOG.runAndLogException { readProjectNameFile(nameFile) }?.let {
        lastSavedProjectName = it
        return it
      }
    }

    return ProjectNameProvider.EP_NAME.extensionList.computeIfAny {
      LOG.runAndLogException { it.getDefaultName(project) }
    } ?: PathUtilRt.getFileName(baseDir).replace(":", "")
  }

  private suspend fun saveProjectName() {
    if (!isDirectoryBased) {
      return
    }

    val currentProjectName = project.name
    if (lastSavedProjectName == currentProjectName) {
      return
    }

    lastSavedProjectName = currentProjectName

    val basePath = projectBasePath

    fun doSave() {
      if (currentProjectName == PathUtilRt.getFileName(basePath)) {
        // name equals to base path name - just remove name
        nameFile.delete()
      }
      else if (Paths.get(basePath).isDirectory()) {
        nameFile.write(currentProjectName.toByteArray())
      }
    }

    try {
      doSave()
    }
    catch (e: AccessDeniedException) {
      val status = ensureFilesWritable(project, listOf(LocalFileSystem.getInstance().refreshAndFindFileByPath(nameFile.systemIndependentPath)!!))
      if (status.hasReadonlyFiles()) {
        throw e
      }

      doSave()
    }
  }

  final override suspend fun doSave(result: SaveResult, forceSavingAllSettings: Boolean) {
    coroutineScope {
      launch {
        // save modules before project
        val errors = SmartList<Throwable>()
        val moduleSaveSessions = saveModules(errors, forceSavingAllSettings)
        result.addErrors(errors)

        (saveSettingsSavingComponentsAndCommitComponents(result, forceSavingAllSettings) as ProjectSaveSessionProducerManager)
          .saveWithAdditionalSaveSessions(moduleSaveSessions)
          .appendTo(result)
      }

      val projectSaved = project.messageBus.syncPublisher(ProjectEx.ProjectSaved.TOPIC)
      launch {
        try {
          saveProjectName()
        }
        catch (e: Throwable) {
          LOG.error("Unable to store project name", e)
        }

        projectSaved.duringSave(project)
      }
    }
  }

  protected open suspend fun saveModules(errors: MutableList<Throwable>, isForceSavingAllSettings: Boolean): List<SaveSession> {
    return emptyList()
  }

  final override fun createSaveSessionProducerManager() = ProjectSaveSessionProducerManager(project)

  final override fun commitObsoleteComponents(session: SaveSessionProducerManager, isProjectLevel: Boolean) {
    if (isDirectoryBased) {
      super.commitObsoleteComponents(session, true)
    }
  }
}

@ApiStatus.Internal
open class ProjectWithModulesStoreImpl(project: Project) : ProjectStoreImpl(project) {
  override suspend fun saveModules(errors: MutableList<Throwable>, isForceSavingAllSettings: Boolean): List<SaveSession> {
    val modules = ModuleManager.getInstance(project)?.modules ?: Module.EMPTY_ARRAY
    if (modules.isEmpty()) {
      return emptyList()
    }

    return withEdtContext {
      // do no create with capacity because very rarely a lot of modules will be modified
      val saveSessions: MutableList<SaveSession> = SmartList<SaveSession>()
      // commit components
      for (module in modules) {
        val moduleStore = ModuleServiceManager.getService(module, IComponentStore::class.java) as ComponentStoreImpl
        // collectSaveSessions is very cheap, so, do it in EDT
        moduleStore.doCreateSaveSessionManagerAndCommitComponents(isForceSavingAllSettings, errors).collectSaveSessions(saveSessions)
      }
      saveSessions
    }
  }
}

internal class PlatformLangProjectStoreFactory : ProjectStoreFactory {
  override fun createStore(project: Project): IComponentStore {
    return if (project.isDefault) DefaultProjectStoreImpl(project) else ProjectWithModulesStoreImpl(project)
  }
}

internal class PlatformProjectStoreFactory : ProjectStoreFactory {
  override fun createStore(project: Project): IComponentStore {
    return if (project.isDefault) DefaultProjectStoreImpl(project) else ProjectStoreImpl(project)
  }
}

@CalledInAny
internal suspend fun ensureFilesWritable(project: Project, files: Collection<VirtualFile>): ReadonlyStatusHandler.OperationStatus {
  return withEdtContext(project) {
    ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(files)
  }
}
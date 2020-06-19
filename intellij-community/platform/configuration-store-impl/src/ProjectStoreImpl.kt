// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.ide.highlighter.ProjectFileType
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.components.impl.stores.IComponentStore
import com.intellij.openapi.components.impl.stores.IProjectStore
import com.intellij.openapi.diagnostic.runAndLogException
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.project.ex.ProjectNameProvider
import com.intellij.openapi.project.impl.ProjectStoreFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtilRt
import com.intellij.util.SmartList
import com.intellij.util.io.delete
import com.intellij.util.io.isDirectory
import com.intellij.util.io.write
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.CalledInAny
import org.jetbrains.jps.util.JpsPathUtil
import java.nio.file.AccessDeniedException
import java.nio.file.Path

internal val IProjectStore.nameFile: Path
  get() = directoryStorePath.resolve(ProjectEx.NAME_FILE)

@ApiStatus.Internal
open class ProjectStoreImpl(project: Project) : ProjectStoreBase(project) {
  private var lastSavedProjectName: String? = null

  init {
    assert(!project.isDefault)
  }

  final override fun getPathMacroManagerForDefaults() = PathMacroManager.getInstance(project)

  override val storageManager = ProjectStateStorageManager(TrackingPathMacroSubstitutorImpl(PathMacroManager.getInstance(project)), project)

  override fun setPath(path: Path) {
    setPath(path, true, null)
  }

  override fun getProjectName(): String {
    if (!isDirectoryBased) {
      return PathUtilRt.getFileName(projectFilePath).removeSuffix(ProjectFileType.DOT_DEFAULT_EXTENSION)
    }

    val projectDir = nameFile.parent
    val storedName = JpsPathUtil.readProjectName(projectDir)
    if (storedName != null) {
      lastSavedProjectName = storedName
      return storedName
    }

    val computedName = ProjectNameProvider.EP_NAME.iterable.asSequence()
      .map { LOG.runAndLogException { it.getDefaultName(project) } }
      .find { it != null }

    return computedName ?: JpsPathUtil.getDefaultProjectName(projectDir)
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
      if (currentProjectName == basePath.fileName.toString()) {
        // name equals to base path name - just remove name
        nameFile.delete()
      }
      else if (basePath.isDirectory()) {
        nameFile.write(currentProjectName.toByteArray())
      }
    }

    try {
      doSave()
    }
    catch (e: AccessDeniedException) {
      val status = ensureFilesWritable(project, listOf(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(nameFile)!!))
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
        val saveSessionManager = createSaveSessionProducerManager()
        val moduleSaveSessions = saveModules(errors, forceSavingAllSettings, saveSessionManager)
        result.addErrors(errors)

        saveSettingsSavingComponentsAndCommitComponents(result, forceSavingAllSettings, saveSessionManager)
        saveSessionManager
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

  protected open suspend fun saveModules(errors: MutableList<Throwable>,
                                         isForceSavingAllSettings: Boolean,
                                         projectSaveSessionManager: SaveSessionProducerManager): List<SaveSession> {
    return emptyList()
  }

  override fun createSaveSessionProducerManager() = ProjectSaveSessionProducerManager(project)

  final override fun commitObsoleteComponents(session: SaveSessionProducerManager, isProjectLevel: Boolean) {
    if (isDirectoryBased) {
      super.commitObsoleteComponents(session, true)
    }
  }
}

@ApiStatus.Internal
open class ProjectWithModulesStoreImpl(project: Project) : ProjectStoreImpl(project) {
  override suspend fun saveModules(errors: MutableList<Throwable>,
                                   isForceSavingAllSettings: Boolean,
                                   projectSaveSessionManager: SaveSessionProducerManager): List<SaveSession> {
    val modules = ModuleManager.getInstance(project)?.modules ?: Module.EMPTY_ARRAY
    if (modules.isEmpty()) {
      return emptyList()
    }

    return withEdtContext {
      // do no create with capacity because very rarely a lot of modules will be modified
      val saveSessions: MutableList<SaveSession> = SmartList()
      // commit components
      for (module in modules) {
        val moduleStore = module.getService(IComponentStore::class.java) as? ComponentStoreImpl ?: continue
        // collectSaveSessions is very cheap, so, do it in EDT
        val saveManager = moduleStore.createSaveSessionProducerManager()
        commitModuleComponents(moduleStore, saveManager, projectSaveSessionManager, isForceSavingAllSettings, errors)
        saveManager.collectSaveSessions(saveSessions)
      }
      saveSessions
    }
  }

  protected open fun commitModuleComponents(moduleStore: ComponentStoreImpl, moduleSaveSessionManager: SaveSessionProducerManager,
                                            projectSaveSessionManager: SaveSessionProducerManager, isForceSavingAllSettings: Boolean,
                                            errors: MutableList<Throwable>) {
    moduleStore.commitComponents(isForceSavingAllSettings, moduleSaveSessionManager, errors)
  }
}

abstract class ProjectStoreFactoryImpl : ProjectStoreFactory {
  final override fun createDefaultProjectStore(project: Project) = DefaultProjectStoreImpl(project)
}

internal class PlatformLangProjectStoreFactory : ProjectStoreFactoryImpl() {
  override fun createStore(project: Project): IProjectStore {
    LOG.assertTrue(!project.isDefault)
    return ProjectWithModulesStoreImpl(project)
  }
}

internal class PlatformProjectStoreFactory : ProjectStoreFactoryImpl() {
  override fun createStore(project: Project): IProjectStore {
    LOG.assertTrue(!project.isDefault)
    return ProjectStoreImpl(project)
  }
}

@CalledInAny
internal suspend fun ensureFilesWritable(project: Project, files: Collection<VirtualFile>): ReadonlyStatusHandler.OperationStatus {
  return withEdtContext(project) {
    ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(files)
  }
}
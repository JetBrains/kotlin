// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.ide.highlighter.ProjectFileType
import com.intellij.ide.highlighter.WorkspaceFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.appSystemDir
import com.intellij.openapi.components.*
import com.intellij.openapi.components.impl.stores.IProjectStore
import com.intellij.openapi.diagnostic.runAndLogException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCoreUtil
import com.intellij.openapi.project.getProjectCacheFileName
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtilRt
import com.intellij.util.SmartList
import com.intellij.util.containers.computeIfAny
import com.intellij.util.containers.isNullOrEmpty
import com.intellij.util.io.exists
import com.intellij.util.io.move
import com.intellij.util.io.systemIndependentPath
import com.intellij.util.text.nullize
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

internal const val PROJECT_FILE = "\$PROJECT_FILE$"
internal const val PROJECT_CONFIG_DIR = "\$PROJECT_CONFIG_DIR$"

internal val PROJECT_FILE_STORAGE_ANNOTATION = FileStorageAnnotation(PROJECT_FILE, false)
private val DEPRECATED_PROJECT_FILE_STORAGE_ANNOTATION = FileStorageAnnotation(PROJECT_FILE, true)

// cannot be `internal`, used in Upsource
abstract class ProjectStoreBase(final override val project: Project) : ComponentStoreWithExtraComponents(), IProjectStore {
  // protected setter used in upsource
  // Zelix KlassMaster - ERROR: Could not find method 'getScheme()'
  var scheme = StorageScheme.DEFAULT

  final override var loadPolicy = StateLoadPolicy.LOAD

  final override fun isOptimiseTestLoadSpeed() = loadPolicy != StateLoadPolicy.LOAD

  final override fun getStorageScheme() = scheme

  abstract override val storageManager: StateStorageManagerImpl

  protected val isDirectoryBased: Boolean
    get() = scheme == StorageScheme.DIRECTORY_BASED

  final override fun setOptimiseTestLoadSpeed(value: Boolean) {
    // we don't load default state in tests as app store does because
    // 1) we should not do it
    // 2) it was so before, so, we preserve old behavior (otherwise RunManager will load template run configurations)
    loadPolicy = if (value) StateLoadPolicy.NOT_LOAD else StateLoadPolicy.LOAD
  }

  override fun getProjectFilePath() = storageManager.expandMacro(PROJECT_FILE)

  /**
   * `null` for default or non-directory based project.
   */
  override fun getProjectConfigDir() = if (isDirectoryBased) storageManager.expandMacro(PROJECT_CONFIG_DIR) else null

  final override fun getWorkspaceFilePath() = storageManager.expandMacro(StoragePathMacros.WORKSPACE_FILE)

  final override fun clearStorages() {
    storageManager.clearStorages()
  }

  private fun loadProjectFromTemplate(defaultProject: Project) {
    runBlocking { defaultProject.stateStore.save() }

    val element = (defaultProject.stateStore as DefaultProjectStoreImpl).getStateCopy() ?: return
    LOG.runAndLogException {
      if (isDirectoryBased) {
        normalizeDefaultProjectElement(defaultProject, element, Paths.get(storageManager.expandMacro(PROJECT_CONFIG_DIR)))
      }
      else {
        moveComponentConfiguration(defaultProject, element) {
          if (it == "workspace.xml") Paths.get(workspaceFilePath)
          else Paths.get(projectFilePath)
        }
      }
    }
    (storageManager.getOrCreateStorage(PROJECT_FILE) as XmlElementStorage).setDefaultState(element)
  }

  final override fun getProjectBasePath(): String {
    if (isDirectoryBased) {
      val path = PathUtilRt.getParentPath(storageManager.expandMacro(PROJECT_CONFIG_DIR))
      if (Registry.`is`("store.basedir.parent.detection", true) && PathUtilRt.getFileName(
          path).startsWith("${Project.DIRECTORY_STORE_FOLDER}.")) {
        return PathUtilRt.getParentPath(PathUtilRt.getParentPath(path))
      }
      return path
    }
    else {
      return PathUtilRt.getParentPath(projectFilePath)
    }
  }

  override fun setPath(filePath: String, isRefreshVfsNeeded: Boolean, template: Project?) {
    val storageManager = storageManager
    val fs = LocalFileSystem.getInstance()
    val isUnitTestMode = ApplicationManager.getApplication().isUnitTestMode
    if (filePath.endsWith(ProjectFileType.DOT_DEFAULT_EXTENSION)) {
      scheme = StorageScheme.DEFAULT

      storageManager.addMacro(PROJECT_FILE, filePath)

      val workspacePath = composeFileBasedProjectWorkSpacePath(filePath)
      storageManager.addMacro(StoragePathMacros.WORKSPACE_FILE, workspacePath)

      if (isRefreshVfsNeeded) {
        VfsUtil.markDirtyAndRefresh(false, true, false, fs.refreshAndFindFileByPath(filePath), fs.refreshAndFindFileByPath(workspacePath))
      }

      if (isUnitTestMode) {
        // load state only if there are existing files
        isOptimiseTestLoadSpeed = !Paths.get(filePath).toFile().exists()
      }
    }
    else {
      scheme = StorageScheme.DIRECTORY_BASED

      val configDir = "$filePath/${Project.DIRECTORY_STORE_FOLDER}"
      storageManager.addMacro(PROJECT_CONFIG_DIR, configDir)
      storageManager.addMacro(PROJECT_FILE, "$configDir/misc.xml")
      storageManager.addMacro(StoragePathMacros.WORKSPACE_FILE, "$configDir/workspace.xml")

      if (isUnitTestMode) {
        // load state only if there are existing files
        isOptimiseTestLoadSpeed = !Paths.get(filePath).exists()
      }

      if (isRefreshVfsNeeded) {
        VfsUtil.markDirtyAndRefresh(false, true, true, fs.refreshAndFindFileByPath(configDir))
      }
    }

    if (template != null) {
      loadProjectFromTemplate(template)
    }

    val cacheFileName = project.getProjectCacheFileName(extensionWithDot = ".xml")
    storageManager.addMacro(StoragePathMacros.CACHE_FILE, appSystemDir.resolve("workspace").resolve(cacheFileName).systemIndependentPath)

    if (isUnitTestMode) {
      storageManager.addMacro(StoragePathMacros.PRODUCT_WORKSPACE_FILE, storageManager.expandMacro(StoragePathMacros.WORKSPACE_FILE))
      return
    }

    val productSpecificWorkspaceParentDir = "${FileUtil.toSystemIndependentName(PathManager.getConfigPath())}/workspace"
    val projectIdManager = ProjectIdManager.getInstance(project)
    var projectId = projectIdManager.state.id
    if (projectId == null) {
      // do not use project name as part of id, to ensure that project dir renaming also will not cause data loss
      projectId = Ksuid.generate()
      projectIdManager.state.id = projectId

      try {
        val oldFile = Paths.get("$productSpecificWorkspaceParentDir/$cacheFileName")
        if (oldFile.exists()) {
          oldFile.move(Paths.get("$productSpecificWorkspaceParentDir/$projectId.xml"))
        }
      }
      catch (e: Exception) {
        LOG.error(e)
      }
    }

    storageManager.addMacro(StoragePathMacros.PRODUCT_WORKSPACE_FILE, "$productSpecificWorkspaceParentDir/$projectId.xml")
  }

  override fun <T> getStorageSpecs(component: PersistentStateComponent<T>, stateSpec: State, operation: StateStorageOperation): List<Storage> {
    val storages = stateSpec.storages
    if (storages.isEmpty()) {
      return listOf(PROJECT_FILE_STORAGE_ANNOTATION)
    }

    if (isDirectoryBased) {
      var result: MutableList<Storage>? = null

      if (storages.size == 2 && ApplicationManager.getApplication().isUnitTestMode &&
          isSpecialStorage(storages.first()) &&
          storages.get(1).path == StoragePathMacros.WORKSPACE_FILE) {
        return listOf(storages.first())
      }

      for (storage in storages) {
        if (storage.path != PROJECT_FILE) {
          if (result == null) {
            result = SmartList()
          }
          result.add(storage)
        }
      }

      if (result.isNullOrEmpty()) {
        return listOf(PROJECT_FILE_STORAGE_ANNOTATION)
      }
      else {
        result!!.sortWith(deprecatedComparator)
        if (isDirectoryBased) {
          StreamProviderFactory.EP_NAME.getExtensions(project).computeIfAny {
            LOG.runAndLogException { it.customizeStorageSpecs(component, storageManager, stateSpec, result!!, operation) }
          }?.let {
              // yes, DEPRECATED_PROJECT_FILE_STORAGE_ANNOTATION is not added in this case
              return it
            }
        }

        // if we create project from default, component state written not to own storage file, but to project file,
        // we don't have time to fix it properly, so, ancient hack restored
        if (!isSpecialStorage(result.first())) {
          result.add(DEPRECATED_PROJECT_FILE_STORAGE_ANNOTATION)
        }
        return result
      }
    }
    else {
      var result: MutableList<Storage>? = null
      // FlexIdeProjectLevelCompilerOptionsHolder, FlexProjectLevelCompilerOptionsHolderImpl and CustomBeanRegistry
      var hasOnlyDeprecatedStorages = true
      for (storage in storages) {
        @Suppress("DEPRECATION")
        if (storage.path == PROJECT_FILE || storage.path == StoragePathMacros.WORKSPACE_FILE || isSpecialStorage(storage)) {
          if (result == null) {
            result = SmartList()
          }
          result.add(storage)
          if (!storage.deprecated) {
            hasOnlyDeprecatedStorages = false
          }
        }
      }
      if (result.isNullOrEmpty()) {
        return listOf(PROJECT_FILE_STORAGE_ANNOTATION)
      }
      else {
        if (hasOnlyDeprecatedStorages) {
          result!!.add(PROJECT_FILE_STORAGE_ANNOTATION)
        }
        result!!.sortWith(deprecatedComparator)
        return result
      }
    }
  }

  override fun isProjectFile(file: VirtualFile): Boolean {
    if (!file.isInLocalFileSystem || !ProjectCoreUtil.isProjectOrWorkspaceFile(file)) {
      return false
    }

    val filePath = file.path
    if (!isDirectoryBased) {
      return filePath == projectFilePath || filePath == workspaceFilePath
    }
    return FileUtil.isAncestor(PathUtilRt.getParentPath(projectFilePath), filePath, false)
  }

  override fun getDirectoryStorePath(ignoreProjectStorageScheme: Boolean): String? {
    return when {
      !ignoreProjectStorageScheme && !isDirectoryBased -> null
      else -> PathUtilRt.getParentPath(projectFilePath).nullize()
    }
  }

  override fun getDirectoryStoreFile(): VirtualFile? = directoryStorePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }

  override fun getDirectoryStorePathOrBase(): String = PathUtilRt.getParentPath(projectFilePath)

  override suspend fun doSave(result: SaveResult, forceSavingAllSettings: Boolean) {
    // do nothing, dummy implementation for Upsource
  }
}

private fun composeFileBasedProjectWorkSpacePath(filePath: String) = "${FileUtilRt.getNameWithoutExtension(filePath)}${WorkspaceFileType.DOT_DEFAULT_EXTENSION}"

private fun isSpecialStorage(storage: Storage) = isSpecialStorage(storage.path)

internal fun isSpecialStorage(collapsedPath: String): Boolean {
  return collapsedPath == StoragePathMacros.CACHE_FILE || collapsedPath == StoragePathMacros.PRODUCT_WORKSPACE_FILE
}
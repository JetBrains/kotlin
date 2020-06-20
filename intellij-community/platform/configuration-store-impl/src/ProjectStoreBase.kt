// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import com.intellij.util.SmartList
import com.intellij.util.containers.isNullOrEmpty
import com.intellij.util.io.Ksuid
import com.intellij.util.io.exists
import com.intellij.util.io.move
import com.intellij.util.io.systemIndependentPath
import com.intellij.util.text.nullize
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.asSequence

internal const val PROJECT_FILE = "\$PROJECT_FILE$"
internal const val PROJECT_CONFIG_DIR = "\$PROJECT_CONFIG_DIR$"

internal val PROJECT_FILE_STORAGE_ANNOTATION = FileStorageAnnotation(PROJECT_FILE, false)
private val DEPRECATED_PROJECT_FILE_STORAGE_ANNOTATION = FileStorageAnnotation(PROJECT_FILE, true)

// cannot be `internal`, used in Upsource
abstract class ProjectStoreBase(final override val project: Project) : ComponentStoreWithExtraComponents(), IProjectStore {
  private var dirOrFile: Path? = null

  // the protected setter used in Upsource
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

  final override fun getProjectFilePath() = storageManager.expandMacro(PROJECT_FILE)

  /**
   * `null` for default or non-directory based project.
   */
  override fun getProjectConfigDir() = if (isDirectoryBased) storageManager.expandMacro(PROJECT_CONFIG_DIR) else null

  final override fun getWorkspaceFilePath() = storageManager.expandMacro(StoragePathMacros.WORKSPACE_FILE)

  final override fun clearStorages() = storageManager.clearStorages()

  private fun loadProjectFromTemplate(defaultProject: Project) {
    val stateStore = defaultProject.stateStore as DefaultProjectStoreImpl
    runBlocking { stateStore.save() }
    val element = stateStore.getStateCopy() ?: return
    LOG.runAndLogException {
      if (isDirectoryBased) {
        normalizeDefaultProjectElement(defaultProject, element, Paths.get(storageManager.expandMacro(PROJECT_CONFIG_DIR)))
      }
      else {
        moveComponentConfiguration(defaultProject, element, { /* doesn't matter, any path will be resolved as projectFilePath (see fileResolver below) */ PROJECT_FILE }) {
          if (it == "workspace.xml") Paths.get(workspaceFilePath)
          else Paths.get(projectFilePath)
        }
      }
    }
  }

  final override fun getProjectBasePath(): Path {
    val path = dirOrFile ?: throw IllegalStateException("setPath was not yet called")
    if (isDirectoryBased) {
      val useParent = Registry.`is`("store.basedir.parent.detection", true) && (path.fileName?.toString()?.startsWith("${Project.DIRECTORY_STORE_FOLDER}.") ?: false)
      return if (useParent) path.parent.parent else path
    }
    else {
      return path.parent
    }
  }

  override fun getProjectWorkspaceId() = ProjectIdManager.getInstance(project).state.id

  override fun setPath(file: Path, isRefreshVfsNeeded: Boolean, template: Project?) {
    dirOrFile = file

    val storageManager = storageManager
    val fs = LocalFileSystem.getInstance()
    val isUnitTestMode = ApplicationManager.getApplication().isUnitTestMode
    val filePath = file.systemIndependentPath
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
        isOptimiseTestLoadSpeed = !file.exists()

        storageManager.addMacro(StoragePathMacros.PRODUCT_WORKSPACE_FILE, workspacePath)
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
        isOptimiseTestLoadSpeed = !file.exists()

        storageManager.addMacro(StoragePathMacros.PRODUCT_WORKSPACE_FILE, "$configDir/product-workspace.xml")
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
          storages[1].path == StoragePathMacros.WORKSPACE_FILE) {
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
          StreamProviderFactory.EP_NAME.extensions(project).asSequence()
            .map { LOG.runAndLogException { it.customizeStorageSpecs(component, storageManager, stateSpec, result!!, operation) } }
            .find { it != null }
            ?.let { return it }  // yes, DEPRECATED_PROJECT_FILE_STORAGE_ANNOTATION is not added in this case
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

    return FileUtil.isAncestor(PathUtil.getParentPath(projectFilePath), filePath, false)
  }

  override fun getDirectoryStorePath(ignoreProjectStorageScheme: Boolean): String? {
    return if (!ignoreProjectStorageScheme && !isDirectoryBased) null else PathUtil.getParentPath(projectFilePath).nullize()
  }

  final override fun getDirectoryStorePath(): Path? {
    return if (isDirectoryBased) dirOrFile!!.resolve(Project.DIRECTORY_STORE_FOLDER) else null
  }

  override fun getDirectoryStorePathOrBase(): String = PathUtil.getParentPath(projectFilePath)

  // dummy implementation for Upsource
  override suspend fun doSave(result: SaveResult, forceSavingAllSettings: Boolean) { }
}

private fun composeFileBasedProjectWorkSpacePath(filePath: String) = "${FileUtil.getNameWithoutExtension(filePath)}${WorkspaceFileType.DOT_DEFAULT_EXTENSION}"

private fun isSpecialStorage(storage: Storage) = isSpecialStorage(storage.path)

internal fun isSpecialStorage(collapsedPath: String): Boolean =
  collapsedPath == StoragePathMacros.CACHE_FILE || collapsedPath == StoragePathMacros.PRODUCT_WORKSPACE_FILE
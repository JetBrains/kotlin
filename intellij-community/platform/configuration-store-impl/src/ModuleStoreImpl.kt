// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.*
import com.intellij.openapi.components.impl.stores.ModuleStore
import com.intellij.openapi.diagnostic.runAndLogException
import com.intellij.openapi.module.Module
import com.intellij.project.isDirectoryBased
import com.intellij.util.containers.computeIfAny
import com.intellij.util.io.exists
import java.nio.file.Paths

private val MODULE_FILE_STORAGE_ANNOTATION = FileStorageAnnotation(StoragePathMacros.MODULE_FILE, false)

private open class ModuleStoreImpl(module: Module, private val pathMacroManager: PathMacroManager) : ModuleStoreBase() {
  override val project = module.project

  override val storageManager = ModuleStateStorageManager(TrackingPathMacroSubstitutorImpl(pathMacroManager), module)

  final override fun getPathMacroManagerForDefaults() = pathMacroManager

  // todo what about Upsource? For now this implemented not in the ModuleStoreBase because `project` and `module` are available only in this class (ModuleStoreImpl)
  override fun <T> getStorageSpecs(component: PersistentStateComponent<T>, stateSpec: State, operation: StateStorageOperation): List<Storage> {
    val result =  super.getStorageSpecs(component, stateSpec, operation)
    if (!project.isDirectoryBased) {
      return result
    }

    return StreamProviderFactory.EP_NAME.getExtensions(project).computeIfAny {
      LOG.runAndLogException { it.customizeStorageSpecs(component, storageManager, stateSpec, result, operation) }
    } ?: result
  }
}

private class TestModuleStore(module: Module, pathMacroManager: PathMacroManager) : ModuleStoreImpl(module, pathMacroManager) {
  private var moduleComponentLoadPolicy: StateLoadPolicy? = null

  override fun setPath(path: String, isNew: Boolean) {
    super.setPath(path, isNew)

    if (!isNew && Paths.get(path).exists()) {
      moduleComponentLoadPolicy = StateLoadPolicy.LOAD
    }
  }

  override val loadPolicy: StateLoadPolicy
    get() = moduleComponentLoadPolicy ?: (project.stateStore as ComponentStoreImpl).loadPolicy
}

// used in upsource
abstract class ModuleStoreBase : ChildlessComponentStore(), ModuleStore {
  abstract override val storageManager: StateStorageManagerImpl

  override fun <T> getStorageSpecs(component: PersistentStateComponent<T>, stateSpec: State, operation: StateStorageOperation): List<Storage> {
    val storages = stateSpec.storages
    return if (storages.isEmpty()) {
      listOf(MODULE_FILE_STORAGE_ANNOTATION)
    }
    else {
      super.getStorageSpecs(component, stateSpec, operation)
    }
  }

  final override fun setPath(path: String) {
    setPath(path, false)
  }

  override fun setPath(path: String, isNew: Boolean) {
    val isMacroAdded = storageManager.addMacro(StoragePathMacros.MODULE_FILE, path)
    // if file not null - update storage
    storageManager.getOrCreateStorage(StoragePathMacros.MODULE_FILE, storageCustomizer = {
      if (this !is FileBasedStorage) {
        // upsource
        return@getOrCreateStorage
      }

      setFile(null, if (isMacroAdded) null else Paths.get(path))
      // ModifiableModuleModel#newModule should always create a new module from scratch
      // https://youtrack.jetbrains.com/issue/IDEA-147530

      if (isMacroAdded) {
        // preload to ensure that we will get FileNotFound error (no module file) during init, and not later in some unexpected place (because otherwise will be loaded by demand)
        preloadStorageData(isNew)
      }
      else {
        storageManager.updatePath(StoragePathMacros.MODULE_FILE, path)
      }
    })
  }
}
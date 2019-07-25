// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.PathMacroSubstitutor
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.StateStorageOperation
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.impl.ComponentManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.isExternalStorageEnabled
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element
import org.jetbrains.annotations.ApiStatus

// extended in upsource
open class ProjectStateStorageManager(macroSubstitutor: PathMacroSubstitutor,
                                      private val project: Project,
                                      useVirtualFileTracker: Boolean = true) : StateStorageManagerImpl(ROOT_TAG_NAME, macroSubstitutor, if (useVirtualFileTracker) project else null) {
  companion object {
    internal const val VERSION_OPTION = "version"
    const val ROOT_TAG_NAME = "project"
  }

  private val fileBasedStorageConfiguration = object : FileBasedStorageConfiguration {
    override val isUseVfsForWrite: Boolean
      get() = true

    override val isUseVfsForRead: Boolean
      get() = project is VirtualFileResolver

    override fun resolveVirtualFile(path: String): VirtualFile? {
      return when (project) {
        is VirtualFileResolver -> project.resolveVirtualFile(path)
        else -> super.resolveVirtualFile(path)
      }
    }
  }

  override fun getFileBasedStorageConfiguration(fileSpec: String): FileBasedStorageConfiguration {
    return when {
      isSpecialStorage(fileSpec) -> appFileBasedStorageConfiguration
      else -> fileBasedStorageConfiguration
    }
  }

  override fun normalizeFileSpec(fileSpec: String) = removeMacroIfStartsWith(super.normalizeFileSpec(fileSpec), PROJECT_CONFIG_DIR)

  override fun expandMacros(path: String): String {
    if (path[0] == '$') {
      return super.expandMacros(path)
    }
    else {
      return "${expandMacro(PROJECT_CONFIG_DIR)}/$path"
    }
  }

  override fun beforeElementSaved(elements: MutableList<Element>, rootAttributes: MutableMap<String, String>) {
    rootAttributes.put(VERSION_OPTION, "4")
  }

  override fun getOldStorageSpec(component: Any, componentName: String, operation: StateStorageOperation): String? {
    val workspace = (project as? ComponentManagerImpl)?.isWorkspaceComponent(component.javaClass) ?: false
    if (workspace && (operation != StateStorageOperation.READ || getOrCreateStorage(StoragePathMacros.WORKSPACE_FILE, RoamingType.DISABLED).hasState(componentName, false))) {
      return StoragePathMacros.WORKSPACE_FILE
    }
    return PROJECT_FILE
  }

  override val isExternalSystemStorageEnabled: Boolean
    get() = project.isExternalStorageEnabled
}

// for upsource
@ApiStatus.Experimental
interface VirtualFileResolver {
  @JvmDefault
  fun resolveVirtualFile(path: String): VirtualFile? {
    return defaultFileBasedStorageConfiguration.resolveVirtualFile(path)
  }
}
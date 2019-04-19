// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.configurationStore

import com.intellij.configurationStore.*
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import org.jdom.Element
import org.jetbrains.jps.model.serialization.SerializationConstants

internal class ExternalModuleStorage(private val module: Module,
                                     storageManager: StateStorageManager) : XmlElementStorage(StoragePathMacros.MODULE_FILE, "module", storageManager.macroSubstitutor, RoamingType.DISABLED) {
  override val isUseVfsForWrite: Boolean
    get() = false

  private val manager = StreamProviderFactory.EP_NAME.getExtensionList(module.project).first { it is ExternalSystemStreamProviderFactory } as ExternalSystemStreamProviderFactory

  override fun loadLocalData() = manager.readModuleData(module.name)

  override fun createSaveSession(states: StateMap) = object : XmlElementStorageSaveSession<ExternalModuleStorage>(states, this) {
    override fun saveLocally(dataWriter: DataWriter?) {
      manager.moduleStorage.write(module.name, dataWriter)
    }
  }
}

internal open class ExternalProjectStorage @JvmOverloads constructor(fileSpec: String,
                                                                     project: Project,
                                                                     storageManager: StateStorageManager,
                                                                     rootElementName: String? = ProjectStateStorageManager.ROOT_TAG_NAME /* several components per file */) : XmlElementStorage(fileSpec,
                                                                                                                                                                                                rootElementName,
                                                                                                                                                                                                storageManager.macroSubstitutor,
                                                                                                                                                                                                RoamingType.DISABLED) {
  override val isUseVfsForWrite: Boolean
    get() = false

  protected val manager = StreamProviderFactory.EP_NAME.getExtensionList(project).first { it is ExternalSystemStreamProviderFactory } as ExternalSystemStreamProviderFactory

  override fun loadLocalData() = manager.fileStorage.read(fileSpec)

  override fun createSaveSession(states: StateMap) = object : XmlElementStorageSaveSession<ExternalProjectStorage>(states, this) {
    override fun saveLocally(dataWriter: DataWriter?) {
      manager.fileStorage.write(fileSpec, dataWriter)
    }
  }
}

// for libraries only for now - we use null rootElementName because the only component is expected (libraryTable)
internal class ExternalProjectFilteringStorage(fileSpec: String,
                                               project: Project,
                                               storageManager: StateStorageManager,
                                               private val componentName: String,
                                               private val inProjectStorage: DirectoryBasedStorage) : ExternalProjectStorage(fileSpec, project, storageManager, rootElementName = null /* the only component per file */) {
  private val filter = DataWriterFilter.requireAttribute(SerializationConstants.EXTERNAL_SYSTEM_ID_ATTRIBUTE, DataWriterFilter.ElementLevel.FIRST)

  override fun loadLocalData(): Element? {
    val externalData = super.loadLocalData()
    val internalData = inProjectStorage.getSerializedState(inProjectStorage.loadData(), null, componentName, true)
    return JDOMUtil.merge(externalData, internalData)
  }

  override fun createSaveSession(states: StateMap) = object : XmlElementStorageSaveSession<ExternalProjectStorage>(states, this) {
    override fun saveLocally(dataWriter: DataWriter?) {
      manager.fileStorage.write(fileSpec, dataWriter, filter)
    }
  }
}
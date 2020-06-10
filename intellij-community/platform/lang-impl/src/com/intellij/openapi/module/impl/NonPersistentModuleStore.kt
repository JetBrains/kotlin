// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.module.impl

import com.intellij.configurationStore.SaveSessionProducer
import com.intellij.configurationStore.StateStorageManager
import com.intellij.configurationStore.StreamProvider
import com.intellij.openapi.components.*
import com.intellij.openapi.components.impl.stores.ModuleStore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.MessageBus
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
internal class NonPersistentModuleStore : ModuleStore {
  override val storageManager: StateStorageManager = NonPersistentStateStorageManager
  override fun setPath(path: String): Unit = Unit
  override fun setPath(path: String, virtualFile: VirtualFile?, isNew: Boolean): Unit = Unit
  override fun initComponent(component: Any, serviceDescriptor: ServiceDescriptor?, pluginId: PluginId?): Unit = Unit
  override fun initPersistencePlainComponent(component: Any, key: String): Unit = Unit
  override fun reloadStates(componentNames: Set<String>, messageBus: MessageBus): Unit = Unit
  override fun reloadState(componentClass: Class<out PersistentStateComponent<*>>): Unit = Unit
  override fun isReloadPossible(componentNames: Set<String>): Boolean = true
  override suspend fun save(forceSavingAllSettings: Boolean): Unit = Unit
  override fun saveComponent(component: PersistentStateComponent<*>): Unit = Unit
}

private object NonPersistentStateStorageManager : StateStorageManager {
  override val componentManager: ComponentManager? = null
  override fun getStateStorage(storageSpec: Storage): StateStorage = NonPersistentStateStorage
  override fun addStreamProvider(provider: StreamProvider, first: Boolean) = Unit
  override fun removeStreamProvider(clazz: Class<out StreamProvider>) = Unit
  override fun rename(path: String, newName: String) = Unit
  override fun getOldStorage(component: Any, componentName: String, operation: StateStorageOperation): StateStorage? = null
  override fun expandMacros(path: String): String = path
}

private object NonPersistentStateStorage : StateStorage {
  override fun <T : Any> getState(component: Any?, componentName: String, stateClass: Class<T>, mergeInto: T?, reload: Boolean): T? = null
  override fun hasState(componentName: String, reloadData: Boolean): Boolean = false
  override fun createSaveSessionProducer(): SaveSessionProducer? = null
  override fun analyzeExternalChangesAndUpdateIfNeeded(componentNames: MutableSet<String>) = Unit
}
// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.ServiceDescriptor
import com.intellij.openapi.components.SettingsSavingComponent
import com.intellij.openapi.diagnostic.runAndLogException
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

// A way to remove obsolete component data.
internal val OBSOLETE_STORAGE_EP = ExtensionPointName<ObsoleteStorageBean>("com.intellij.obsoleteStorage")

abstract class ComponentStoreWithExtraComponents : ComponentStoreImpl() {
  @Suppress("DEPRECATION")
  private val settingsSavingComponents = ContainerUtil.createLockFreeCopyOnWriteList<SettingsSavingComponent>()
  private val asyncSettingsSavingComponents = ContainerUtil.createLockFreeCopyOnWriteList<com.intellij.configurationStore.SettingsSavingComponent>()

  // todo do we really need this?
  private val isSaveSettingsInProgress = AtomicBoolean()

  override suspend fun save(forceSavingAllSettings: Boolean) {
    if (!isSaveSettingsInProgress.compareAndSet(false, true)) {
      LOG.warn("save call is ignored because another save in progress", Throwable())
      return
    }

    try {
      super.save(forceSavingAllSettings)
    }
    finally {
      isSaveSettingsInProgress.set(false)
    }
  }

  override fun initComponent(component: Any, serviceDescriptor: ServiceDescriptor?) {
    @Suppress("DEPRECATION")
    if (component is com.intellij.configurationStore.SettingsSavingComponent) {
      asyncSettingsSavingComponents.add(component)
    }
    else if (component is SettingsSavingComponent) {
      settingsSavingComponents.add(component)
    }

    super.initComponent(component, serviceDescriptor)
  }

  internal suspend fun saveSettingsSavingComponentsAndCommitComponents(result: SaveResult, forceSavingAllSettings: Boolean,
                                                                       saveSessionProducerManager: SaveSessionProducerManager) {
    coroutineScope {
      // expects EDT
      launch(storeEdtCoroutineDispatcher) {
        @Suppress("Duplicates")
        val errors = SmartList<Throwable>()
        for (settingsSavingComponent in settingsSavingComponents) {
          runAndCollectException(errors) {
            settingsSavingComponent.save()
          }
        }
        result.addErrors(errors)
      }

      launch {
        val errors = SmartList<Throwable>()
        for (settingsSavingComponent in asyncSettingsSavingComponents) {
          runAndCollectException(errors) {
            settingsSavingComponent.save()
          }
        }
        result.addErrors(errors)
      }
    }

    // SchemeManager (old settingsSavingComponent) must be saved before saving components (component state uses scheme manager in an ipr project, so, we must save it before)
    // so, call sequentially it, not inside coroutineScope
    commitComponentsOnEdt(result, forceSavingAllSettings, saveSessionProducerManager)
  }

  override fun commitComponents(isForce: Boolean, session: SaveSessionProducerManager, errors: MutableList<Throwable>) {
    // ensure that this task will not interrupt regular saving
    LOG.runAndLogException {
      commitObsoleteComponents(session, false)
    }

    super.commitComponents(isForce, session, errors)
  }

  internal open fun commitObsoleteComponents(session: SaveSessionProducerManager, isProjectLevel: Boolean) {
    for (bean in OBSOLETE_STORAGE_EP.extensionList) {
      if (bean.isProjectLevel != isProjectLevel) {
        continue
      }

      val storage = (storageManager as StateStorageManagerImpl).getOrCreateStorage(bean.file ?: continue)
      for (componentName in bean.components) {
        session.getProducer(storage)?.setState(null, componentName, null)
      }
    }
  }
}

private inline fun <T> runAndCollectException(errors: MutableList<Throwable>, runnable: () -> T): T? {
  try {
    return runnable()
  }
  catch (e: ProcessCanceledException) {
    throw e
  }
  catch (e: Throwable) {
    errors.add(e)
    return null
  }
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.diagnostic.PluginException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.StateStorage
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.isEmpty
import org.jdom.Element
import org.jetbrains.annotations.ApiStatus

abstract class StorageBaseEx<T : Any> : StateStorageBase<T>() {
  internal fun <S : Any> createGetSession(component: PersistentStateComponent<S>, componentName: String, stateClass: Class<S>, reload: Boolean = false): StateGetter<S> {
    return StateGetterImpl(component, componentName, getStorageData(reload), stateClass, this)
  }

  /**
   * serializedState is null if state equals to default (see XmlSerializer.serializeIfNotDefault)
   */
  abstract fun archiveState(storageData: T, componentName: String, serializedState: Element?)
}

internal fun <S : Any> createStateGetter(isUseLoadedStateAsExisting: Boolean, storage: StateStorage, component: PersistentStateComponent<S>, componentName: String, stateClass: Class<S>, reloadData: Boolean): StateGetter<S> {
  if (isUseLoadedStateAsExisting && storage is StorageBaseEx<*>) {
    return storage.createGetSession(component, componentName, stateClass, reloadData)
  }

  return object : StateGetter<S> {
    override fun getState(mergeInto: S?): S? {
      return storage.getState(component, componentName, stateClass, mergeInto, reloadData)
    }

    override fun archiveState(): S? = null
  }
}

@ApiStatus.Internal
interface StateGetter<S : Any> {
  fun getState(mergeInto: S? = null): S?

  fun archiveState(): S?
}

private class StateGetterImpl<S : Any, T : Any>(private val component: PersistentStateComponent<S>,
                                                private val componentName: String,
                                                private val storageData: T,
                                                private val stateClass: Class<S>,
                                                private val storage: StorageBaseEx<T>) : StateGetter<S> {
  private var serializedState: Element? = null

  override fun getState(mergeInto: S?): S? {
    LOG.assertTrue(serializedState == null)

    serializedState = storage.getSerializedState(storageData, component, componentName, archive = false)
    return storage.deserializeState(serializedState, stateClass, mergeInto)
  }

  override fun archiveState() : S? {
    if (serializedState == null) {
      return null
    }

    val stateAfterLoad = try {
      component.state
    }
    catch (e: ProcessCanceledException) {
      throw e
    }
    catch (e: Throwable) {
      PluginException.logPluginError(LOG, "Cannot get state after load", e, component.javaClass)
      null
    }

    val serializedStateAfterLoad = if (stateAfterLoad == null) {
      serializedState
    }
    else {
      serializeState(stateAfterLoad)?.normalizeRootName().let {
        if (it.isEmpty()) null else it
      }
    }

    if (ApplicationManager.getApplication().isUnitTestMode &&
      serializedState != serializedStateAfterLoad &&
      (serializedStateAfterLoad == null || !JDOMUtil.areElementsEqual(serializedState, serializedStateAfterLoad))) {
      LOG.debug("$componentName (from ${component.javaClass.name}) state changed after load. \nOld: ${JDOMUtil.writeElement(serializedState!!)}\n\nNew: ${serializedStateAfterLoad?.let { JDOMUtil.writeElement(it) } ?: "null"}\n")
    }

    storage.archiveState(storageData, componentName, serializedStateAfterLoad)
    return stateAfterLoad
  }
}
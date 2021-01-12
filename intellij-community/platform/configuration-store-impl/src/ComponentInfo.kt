// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.*
import com.intellij.openapi.util.ModificationTracker
import com.intellij.util.ThreeState
import java.util.concurrent.TimeUnit

internal fun createComponentInfo(component: Any, stateSpec: State?, serviceDescriptor: ServiceDescriptor?): ComponentInfo {
  val result = when (component) {
    is PersistentStateComponentWithModificationTracker<*> -> ComponentWithStateModificationTrackerInfo(component, stateSpec, serviceDescriptor?.configurationSchemaKey)
    is ModificationTracker -> ComponentWithModificationTrackerInfo(component, stateSpec, serviceDescriptor?.configurationSchemaKey)
    else -> ComponentInfoImpl(component, stateSpec)
  }

  if (stateSpec != null && stateSpec.storages.isNotEmpty() && stateSpec.storages.all { it.deprecated || isUseSaveThreshold(it) }) {
    result.lastSaved = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).toInt()
  }
  return result
}

private fun isUseSaveThreshold(storage: Storage): Boolean {
  return storage.useSaveThreshold != ThreeState.NO && getEffectiveRoamingType(storage.roamingType, storage.path) === RoamingType.DISABLED
}

abstract class ComponentInfo {
  open val configurationSchemaKey: String?
    get() = null

  abstract val component: Any
  abstract val stateSpec: State?

  abstract val lastModificationCount: Long
  abstract val currentModificationCount: Long

  abstract val isModificationTrackingSupported: Boolean

  var lastSaved: Int = -1

  var affectedPropertyNames: List<String> = emptyList()

  open fun updateModificationCount(newCount: Long) {
  }
}

internal class ComponentInfoImpl(override val component: Any, override val stateSpec: State?) : ComponentInfo() {
  override val isModificationTrackingSupported = false

  override val lastModificationCount: Long
    get() = -1

  override val currentModificationCount: Long
    get() = -1
}

private abstract class ModificationTrackerAwareComponentInfo : ComponentInfo() {
  final override val isModificationTrackingSupported = true

  abstract override var lastModificationCount: Long

  final override fun updateModificationCount(newCount: Long) {
    lastModificationCount = newCount
  }
}

private class ComponentWithStateModificationTrackerInfo(override val component: PersistentStateComponentWithModificationTracker<*>,
                                                        override val stateSpec: State?,
                                                        override val configurationSchemaKey: String?) : ModificationTrackerAwareComponentInfo() {
  override val currentModificationCount: Long
    get() = component.stateModificationCount

  override var lastModificationCount = currentModificationCount
}

private class ComponentWithModificationTrackerInfo(override val component: ModificationTracker,
                                                   override val stateSpec: State?,
                                                   override val configurationSchemaKey: String?) : ModificationTrackerAwareComponentInfo() {
  override val currentModificationCount: Long
    get() = component.modificationCount

  override var lastModificationCount = currentModificationCount
}
// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.PersistentStateComponentWithModificationTracker
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.ModificationTracker
import com.intellij.util.ThreeState
import java.util.concurrent.TimeUnit

internal fun createComponentInfo(component: Any, stateSpec: State?): ComponentInfo {
  return when (component) {
    is ModificationTracker -> ComponentWithModificationTrackerInfo(component, stateSpec)
    is PersistentStateComponentWithModificationTracker<*> -> ComponentWithStateModificationTrackerInfo(component, stateSpec!!)
    else -> {
      val componentInfo = ComponentInfoImpl(component, stateSpec)
      if (stateSpec != null && !stateSpec.storages.isEmpty() && stateSpec.storages.all(::isUseSaveThreshold)) {
        componentInfo.lastSaved = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).toInt()
      }
      componentInfo
    }
  }
}

private fun isUseSaveThreshold(storage: Storage): Boolean {
  return storage.useSaveThreshold != ThreeState.NO && getEffectiveRoamingType(storage.roamingType, storage.path) === RoamingType.DISABLED
}

internal abstract class ComponentInfo {
  abstract val component: Any
  abstract val stateSpec: State?

  abstract val lastModificationCount: Long
  abstract val currentModificationCount: Long

  abstract val isModificationTrackingSupported: Boolean

  var lastSaved: Int = -1

  open fun updateModificationCount(newCount: Long = currentModificationCount) {
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
                                                        override val stateSpec: State) : ModificationTrackerAwareComponentInfo() {
  override val currentModificationCount: Long
    get() = component.stateModificationCount

  override var lastModificationCount = currentModificationCount
}

private class ComponentWithModificationTrackerInfo(override val component: ModificationTracker,
                                                   override val stateSpec: State?) : ModificationTrackerAwareComponentInfo() {
  override val currentModificationCount: Long
    get() = component.modificationCount

  override var lastModificationCount = currentModificationCount
}
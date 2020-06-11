// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros.WORKSPACE_FILE
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTrackerSettings.AutoReloadType
import com.intellij.openapi.observable.properties.AtomicLazyProperty
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

@State(name = "AutoImportSettings", storages = [Storage(WORKSPACE_FILE)])
class AutoImportProjectTrackerSettings : ExternalSystemProjectTrackerSettings, PersistentStateComponent<AutoImportProjectTrackerSettings.State> {

  internal val autoReloadTypeProperty = AtomicLazyProperty { AutoReloadType.SELECTIVE }

  override var autoReloadType by autoReloadTypeProperty

  override fun getState() = State(autoReloadType)

  override fun loadState(state: State) {
    autoReloadType = state.autoReloadType ?: AutoReloadType.SELECTIVE
  }

  data class State(var autoReloadType: AutoReloadType? = null)

  companion object {
    @JvmStatic
    @ApiStatus.Internal
    fun getInstance(project: Project): AutoImportProjectTrackerSettings {
      return ExternalSystemProjectTrackerSettings.getInstance(project) as AutoImportProjectTrackerSettings
    }
  }
}
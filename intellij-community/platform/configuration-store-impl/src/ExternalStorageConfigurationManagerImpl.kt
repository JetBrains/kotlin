// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.project

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.util.ModificationTracker
import com.intellij.util.xmlb.annotations.Property

@Property(style = Property.Style.ATTRIBUTE)
class ExternalStorageConfiguration : BaseState() {
  var enabled by property(false)
}

@State(name = "ExternalStorageConfigurationManager")
internal class ExternalStorageConfigurationManagerImpl : PersistentStateComponent<ExternalStorageConfiguration>, ModificationTracker, ExternalStorageConfigurationManager {
  private var state = ExternalStorageConfiguration()

  override fun getModificationCount(): Long = state.modificationCount

  override fun getState(): ExternalStorageConfiguration {
    return state
  }

  override fun loadState(state: ExternalStorageConfiguration) {
    this.state = state
  }

  override fun isEnabled(): Boolean = state.enabled

  /**
   * Internal use only. Call ExternalProjectsManagerImpl.setStoreExternally instead.
   */
  override fun setEnabled(value: Boolean) {
    state.enabled = value
  }
}
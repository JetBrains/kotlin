// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.project

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.util.xmlb.annotations.Property

@Property(style = Property.Style.ATTRIBUTE)
class ExternalStorageConfiguration : BaseState() {
  var enabled by property(false)
}

/**
 * This class isn't used in the new implementation of project model, which is based on [Workspace Model][com.intellij.workspaceModel.ide].
 * It shouldn't be used directly, its interface [ExternalStorageConfigurationManager] should be used instead.
 */
@State(name = "ExternalStorageConfigurationManager")
internal class ExternalStorageConfigurationManagerImpl : SimplePersistentStateComponent<ExternalStorageConfiguration>(ExternalStorageConfiguration()), ExternalStorageConfigurationManager {
  override fun isEnabled(): Boolean = state.enabled

  /**
   * Internal use only. Call ExternalProjectsManagerImpl.setStoreExternally instead.
   */
  override fun setEnabled(value: Boolean) {
    state.enabled = value
  }
}
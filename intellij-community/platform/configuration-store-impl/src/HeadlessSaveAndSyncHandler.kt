// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager

/**
 * Trivial implementation used in tests and in the headless mode.
 */
internal class HeadlessSaveAndSyncHandler : BaseSaveAndSyncHandler() {
  override fun scheduleSave(task: SaveTask, forceExecuteImmediately: Boolean) {}

  override fun scheduleRefresh() {}

  override fun refreshOpenFiles() {}

  override fun blockSaveOnFrameDeactivation() {}

  override fun unblockSaveOnFrameDeactivation() {}

  override fun blockSyncOnFrameActivation() {}

  override fun unblockSyncOnFrameActivation() {}

  override fun saveSettingsUnderModalProgress(componentManager: ComponentManager, isSaveAppAlso: Boolean): Boolean {
    StoreUtil.saveSettings(componentManager, forceSavingAllSettings = true)
    if (isSaveAppAlso && componentManager !== ApplicationManager.getApplication()) {
      StoreUtil.saveSettings(ApplicationManager.getApplication(), forceSavingAllSettings = true)
    }
    return true
  }
}

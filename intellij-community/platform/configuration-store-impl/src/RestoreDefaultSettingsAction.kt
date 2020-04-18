// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ConfigImportHelper
import com.intellij.openapi.application.CustomConfigMigrationOption
import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import java.nio.file.Path

private class RestoreDefaultSettingsAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    if (!confirmRestoreSettings(e, ConfigImportHelper.getBackupPath())) {
      return
    }

    CustomConfigMigrationOption.StartWithCleanConfig.writeConfigMarkerFile()

    invokeLater {
      (ApplicationManager.getApplication() as ApplicationEx).restart(true)
    }
  }

  private fun confirmRestoreSettings(e: AnActionEvent, backupPath: Path?): Boolean {
    val restartButtonText =
      if (ApplicationManager.getApplication().isRestartCapable)
        ConfigurationStoreBundle.message("restore.default.settings.confirmation.button.restart")
      else ConfigurationStoreBundle.message("restore.default.settings.confirmation.button.shutdown")

    return Messages.YES == Messages.showYesNoDialog(
      e.project,
      ConfigurationStoreBundle.message("restore.default.settings.confirmation.message", backupPath),
      ConfigurationStoreBundle.message("restore.default.settings.confirmation.title"),
      restartButtonText,
      CommonBundle.getCancelButtonText(), Messages.getWarningIcon()
    )
  }
}
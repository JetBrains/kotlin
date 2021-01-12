// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.settings.ParameterNameHintsSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.extensions.ExtensionNotApplicableException
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

internal class ParameterHintsSettingsMigration : StartupActivity {
  init {
    if (ApplicationManager.getApplication().isUnitTestMode) {
      throw ExtensionNotApplicableException.INSTANCE
    }
  }

  override fun runActivity(project: Project) {
    val editorSettingsExternalizable = EditorSettingsExternalizable.getInstance()
    if (!editorSettingsExternalizable.isShowParameterNameHints) {
      editorSettingsExternalizable.isShowParameterNameHints = true
      for (language in getBaseLanguagesWithProviders()) {
        ParameterNameHintsSettings.getInstance().setIsEnabledForLanguage(false, language)
      }
    }
  }
}
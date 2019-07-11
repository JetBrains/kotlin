// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.settings.ParameterNameHintsSettings
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class ParameterHintsSettingsMigration : StartupActivity {
  override fun runActivity(project: Project) {
    if (!EditorSettingsExternalizable.getInstance().isShowParameterNameHints) {
      EditorSettingsExternalizable.getInstance().isShowParameterNameHints = true
      for (language in getBaseLanguagesWithProviders()) {
        ParameterNameHintsSettings.getInstance().setIsEnabledForLanguage(false, language)
      }
    }
  }
}
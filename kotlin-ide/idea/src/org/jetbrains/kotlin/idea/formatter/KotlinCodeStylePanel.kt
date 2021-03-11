/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import org.jetbrains.kotlin.idea.KotlinLanguage

class KotlinCodeStylePanel(currentSettings: CodeStyleSettings, settings: CodeStyleSettings) :
    TabbedLanguageCodeStylePanel(KotlinLanguage.INSTANCE, currentSettings, settings) {
    override fun initTabs(settings: CodeStyleSettings) {
        super.initTabs(settings)

        addTab(ImportSettingsPanelWrapper(settings))
        addTab(KotlinOtherSettingsPanel(settings))
        for (provider in CodeStyleSettingsProvider.EXTENSION_POINT_NAME.extensions) {
            if (provider.language == KotlinLanguage.INSTANCE && !provider.hasSettingsPage()) {
                createTab(provider)
            }
        }

        addTab(KotlinSaveStylePanel(settings))
    }
}

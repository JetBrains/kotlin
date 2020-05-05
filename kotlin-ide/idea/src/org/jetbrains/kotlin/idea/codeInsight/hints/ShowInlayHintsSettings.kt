/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.hints

import com.intellij.codeInsight.hints.settings.InlayHintsConfigurable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class ShowInlayHintsSettings : AnAction("Hints Settings...") {
    override fun actionPerformed(e: AnActionEvent) {
        val file = CommonDataKeys.PSI_FILE.getData(e.dataContext) ?: return
        val fileLanguage = file.language
        InlayHintsConfigurable.showSettingsDialogForLanguage(
            file.project,
            fileLanguage
        )
    }
}
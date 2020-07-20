/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.formatter.kotlinCustomSettings
import org.jetbrains.kotlin.idea.formatter.trailingComma.canAddTrailingCommaWithRegistryCheck
import org.jetbrains.kotlin.psi.KtElement

class TrailingCommaIntention : SelfTargetingIntention<KtElement>(
    KtElement::class.java,
    KotlinBundle.lazyMessage("intention.trailing.comma.text")
), LowPriorityAction {
    override fun applyTo(element: KtElement, editor: Editor?) {
        val kotlinCustomSettings = CodeStyle.getSettings(element.containingKtFile).kotlinCustomSettings
        kotlinCustomSettings.ALLOW_TRAILING_COMMA = !kotlinCustomSettings.ALLOW_TRAILING_COMMA
        CodeStyleSettingsManager.getInstance(element.project).notifyCodeStyleSettingsChanged()
    }

    override fun isApplicableTo(element: KtElement, caretOffset: Int): Boolean = element.canAddTrailingCommaWithRegistryCheck().also {
        val actionNumber = 1.takeIf { CodeStyle.getSettings(element.containingKtFile).kotlinCustomSettings.ALLOW_TRAILING_COMMA } ?: 0
        setTextGetter(KotlinBundle.lazyMessage("intention.trailing.comma.custom.text", actionNumber))
    }
}

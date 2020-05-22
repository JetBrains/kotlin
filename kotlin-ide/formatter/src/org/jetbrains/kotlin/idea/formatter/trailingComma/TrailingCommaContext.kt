/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter.trailingComma

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.psi.KtElement

class TrailingCommaContext private constructor(val element: PsiElement, val state: TrailingCommaState) {
    /**
     * Return [KtElement] if [state] != [TrailingCommaState.NOT_APPLICABLE]
     */
    val ktElement: KtElement get() = element as? KtElement ?: error("State is NOT_APPLICABLE")

    companion object {
        fun create(element: PsiElement): TrailingCommaContext = TrailingCommaContext(
            element,
            TrailingCommaState.stateForElement(element),
        )
    }
}

fun TrailingCommaContext.commaExistsOrMayExist(settings: KotlinCodeStyleSettings): Boolean = when (state) {
    TrailingCommaState.EXISTS -> true
    TrailingCommaState.MISSING -> settings.addTrailingCommaIsAllowedFor(element)
    else -> false
}

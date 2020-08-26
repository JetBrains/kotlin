/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.refactoring.HelpID
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction

abstract class AbstractKotlinInlineFunctionHandler<T : KtFunction> : KotlinInlineActionHandler() {
    override val helpId: String get() = HelpID.INLINE_METHOD

    override val refactoringName: String get() = KotlinBundle.message("title.inline.function")

    final override fun canInlineKotlinElement(element: KtElement): Boolean = element is KtFunction && canInlineKotlinFunction(element)

    final override fun inlineKotlinElement(project: Project, editor: Editor?, element: KtElement) {
        @Suppress("UNCHECKED_CAST")
        inlineKotlinFunction(project, editor, element as T)
    }

    abstract fun canInlineKotlinFunction(function: KtFunction): Boolean

    abstract fun inlineKotlinFunction(project: Project, editor: Editor?, function: T)
}

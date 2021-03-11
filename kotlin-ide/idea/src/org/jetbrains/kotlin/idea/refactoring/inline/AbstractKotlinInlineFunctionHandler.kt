/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.refactoring.HelpID
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction

abstract class AbstractKotlinInlineFunctionHandler<T : KtFunction> : KotlinInlineActionHandler() {
    override val helpId: String get() = HelpID.INLINE_METHOD

    override val refactoringName: String get() = KotlinBundle.message("title.inline.function")

    final override fun canInlineKotlinElement(element: KtElement): Boolean = element is KtFunction && canInlineKotlinFunction(element)

    final override fun inlineKotlinElement(project: Project, editor: Editor?, element: KtElement) {
        @Suppress("UNCHECKED_CAST")
        val function = element as T

        if (!checkSources(project, editor, function)) return

        if (!function.hasBody()) {
            val message = when {
                function.isAbstract() -> KotlinBundle.message("refactoring.cannot.be.applied.to.abstract.declaration", refactoringName)
                function.isExpectDeclaration() -> KotlinBundle.message(
                    "refactoring.cannot.be.applied.to.expect.declaration",
                    refactoringName
                )

                else -> KotlinBundle.message("refactoring.cannot.be.applied.no.sources.attached", refactoringName)
            }

            return showErrorHint(project, editor, message)
        }

        inlineKotlinFunction(project, editor, function)
    }

    abstract fun canInlineKotlinFunction(function: KtFunction): Boolean

    abstract fun inlineKotlinFunction(project: Project, editor: Editor?, function: T)
}

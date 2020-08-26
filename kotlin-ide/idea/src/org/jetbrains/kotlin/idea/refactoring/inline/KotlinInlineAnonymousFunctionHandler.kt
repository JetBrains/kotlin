/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.util.parents
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParenthesizedExpression

class KotlinInlineAnonymousFunctionHandler : AbstractKotlinInlineFunctionHandler<KtNamedFunction>() {
    override fun canInlineKotlinFunction(function: KtFunction): Boolean = function is KtNamedFunction && function.name == null

    override fun inlineKotlinFunction(project: Project, editor: Editor?, function: KtNamedFunction) {
        val call = function.parents.takeWhile { it is KtParenthesizedExpression }.lastOrNull()?.parent as? KtCallExpression
        if (call == null) {
            val message = KotlinBundle.message("refactoring.cannot.be.applied.to.anonymous.function.without.invocation", refactoringName)
            return showErrorHint(project, editor, message)
        }

        val message = KotlinBundle.message("refactoring.cannot.be.applied.to.anonymous.function", refactoringName)
        return showErrorHint(project, editor, message)
    }
}

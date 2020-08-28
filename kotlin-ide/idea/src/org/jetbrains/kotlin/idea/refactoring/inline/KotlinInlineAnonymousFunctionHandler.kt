/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.util.isAnonymousFunction
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtNamedFunction

class KotlinInlineAnonymousFunctionHandler : AbstractKotlinInlineFunctionHandler<KtFunction>() {
    override fun canInlineKotlinFunction(function: KtFunction): Boolean = function.isAnonymousFunction || function is KtFunctionLiteral

    override fun inlineKotlinFunction(project: Project, editor: Editor?, function: KtFunction) {
        val call = KotlinInlineAnonymousFunctionProcessor.findCallExpression(function)
        if (call == null) {
            val message = if (function is KtFunctionLiteral)
                KotlinBundle.message("refactoring.cannot.be.applied.to.lambda.expression.without.invocation", refactoringName)
            else
                KotlinBundle.message("refactoring.cannot.be.applied.to.anonymous.function.without.invocation", refactoringName)

            return showErrorHint(project, editor, message)
        }

        KotlinInlineAnonymousFunctionProcessor(function, call, editor, project).run()
    }
}

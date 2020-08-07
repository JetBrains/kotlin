/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.help.HelpManager
import com.intellij.openapi.project.Project
import com.intellij.refactoring.HelpID
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtNamedFunction

class KotlinInlineFunctionDialog(
    project: Project,
    function: KtNamedFunction,
    reference: KtSimpleNameReference?,
    private val replacementStrategy: UsageReplacementStrategy,
    private val allowInlineThisOnly: Boolean
) : AbstractKotlinInlineDialog(function, reference, project) {

    init {
        init()
    }

    override fun isInlineThis() = KotlinRefactoringSettings.instance.INLINE_METHOD_THIS

    public override fun doAction() {
        invokeRefactoring(
          AbstractKotlinInlineDeclarationProcessor(
                project, replacementStrategy, callable, reference,
                inlineThisOnly = isInlineThisOnly || allowInlineThisOnly,
                deleteAfter = !isInlineThisOnly && !isKeepTheDeclaration && !allowInlineThisOnly
            )
        )

        val settings = KotlinRefactoringSettings.instance
        if (myRbInlineThisOnly.isEnabled && myRbInlineAll.isEnabled) {
            settings.INLINE_METHOD_THIS = isInlineThisOnly
        }
    }

    override fun doHelpAction() =
        HelpManager.getInstance().invokeHelp(if (callable is KtConstructor<*>) HelpID.INLINE_CONSTRUCTOR else HelpID.INLINE_METHOD)

    override fun canInlineThisOnly() = allowInlineThisOnly
}

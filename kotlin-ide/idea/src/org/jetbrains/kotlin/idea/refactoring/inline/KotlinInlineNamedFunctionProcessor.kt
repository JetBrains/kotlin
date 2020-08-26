/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.idea.codeInliner.CallableUsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtNamedFunction

class KotlinInlineFunctionProcessor(
    declaration: KtNamedFunction,
    reference: PsiReference?,
    inlineThisOnly: Boolean,
    deleteAfter: Boolean,
    editor: Editor?,
) : AbstractKotlinInlineDeclarationProcessor<KtNamedFunction>(
    declaration = declaration,
    reference = reference,
    inlineThisOnly = inlineThisOnly,
    deleteAfter = deleteAfter,
    editor = editor
) {
    override fun createReplacementStrategy(): UsageReplacementStrategy? = createUsageReplacementStrategyForFunction(declaration, editor)
}

fun createUsageReplacementStrategyForFunction(function: KtNamedFunction, editor: Editor?): UsageReplacementStrategy? {
    val codeToInline = buildCodeToInline(
        function,
        function.bodyExpression!!,
        function.hasBlockBody(),
        editor
    ) ?: return null

    return CallableUsageReplacementStrategy(codeToInline, inlineSetter = false)
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.idea.codeInliner.CallableUsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.CodeToInline
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.psi.KtNamedFunction

class KotlinInlineFunctionProcessor(
    declaration: KtNamedFunction,
    reference: PsiReference?,
    inlineThisOnly: Boolean,
    deleteAfter: Boolean,
    editor: Editor?,
    project: Project,
) : AbstractKotlinInlineNamedDeclarationProcessor<KtNamedFunction>(
    declaration = declaration,
    reference = reference,
    inlineThisOnly = inlineThisOnly,
    deleteAfter = deleteAfter,
    editor = editor,
    project = project,
) {
    override fun createReplacementStrategy(): UsageReplacementStrategy? = createUsageReplacementStrategyForFunction(declaration, editor)
}

fun createUsageReplacementStrategyForFunction(
    function: KtNamedFunction,
    editor: Editor?,
    fallbackToSuperCall: Boolean = false,
): UsageReplacementStrategy? {
    val codeToInline = createCodeToInlineForFunction(function, editor, fallbackToSuperCall = fallbackToSuperCall) ?: return null
    return CallableUsageReplacementStrategy(codeToInline, inlineSetter = false)
}

fun createCodeToInlineForFunction(
    function: KtNamedFunction,
    editor: Editor?,
    fallbackToSuperCall: Boolean = false,
): CodeToInline? = buildCodeToInline(
    function,
    function.bodyExpression!!,
    function.hasBlockBody(),
    editor,
    fallbackToSuperCall,
)

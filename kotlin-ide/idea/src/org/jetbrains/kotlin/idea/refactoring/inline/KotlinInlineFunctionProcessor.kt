/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInliner.CallableUsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KotlinInlineFunctionProcessor(
    declaration: KtNamedFunction,
    reference: KtSimpleNameReference?,
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
    override fun createReplacementStrategy(declaration: KtNamedFunction, editor: Editor?): UsageReplacementStrategy? {
        return createUsageReplacementStrategyForFunction(declaration, editor)
    }
}

fun createUsageReplacementStrategyForFunction(function: KtNamedFunction, editor: Editor?): UsageReplacementStrategy? {
    val returnType = function.unsafeResolveToDescriptor().safeAs<SimpleFunctionDescriptor>()?.returnType
    val codeToInline = buildCodeToInline(
        function,
        returnType,
        function.hasDeclaredReturnType() || (function.hasBlockBody() && returnType?.isUnit() == true),
        function.bodyExpression!!,
        function.hasBlockBody(),
        editor
    ) ?: return null

    return CallableUsageReplacementStrategy(codeToInline, inlineSetter = false)
}
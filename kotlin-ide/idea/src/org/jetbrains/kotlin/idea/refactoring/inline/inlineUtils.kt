/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.openapi.editor.Editor
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInliner.CodeToInline
import org.jetbrains.kotlin.idea.codeInliner.CodeToInlineBuilder
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

internal fun buildCodeToInline(
    declaration: KtDeclaration,
    returnType: KotlinType?,
    isReturnTypeExplicit: Boolean,
    bodyOrInitializer: KtExpression,
    isBlockBody: Boolean,
    editor: Editor?
): CodeToInline? {
    val scope by lazy { bodyOrInitializer.getResolutionScope() }
    fun analyzeExpressionInContext(expression: KtExpression): BindingContext = expression.analyzeInContext(
        scope = scope,
        contextExpression = bodyOrInitializer,
        expectedType = if (isReturnTypeExplicit && (!isBlockBody || expression.parent is KtReturnExpression))
            returnType ?: TypeUtils.NO_EXPECTED_TYPE
        else
            TypeUtils.NO_EXPECTED_TYPE
    )

    val bodyCopy = bodyOrInitializer.copied()
    val descriptor = declaration.unsafeResolveToDescriptor()
    val builder = CodeToInlineBuilder(descriptor as CallableDescriptor, declaration.getResolutionFacade())
    if (isBlockBody) {
        bodyCopy as KtBlockExpression
        val statements = bodyCopy.statements

        val returnStatements = bodyCopy.collectDescendantsOfType<KtReturnExpression> {
            val function = it.getStrictParentOfType<KtFunction>()
            if (function != null && function != declaration) return@collectDescendantsOfType false
            it.getLabelName().let { label -> label == null || label == declaration.name }
        }

        val lastReturn = statements.lastOrNull() as? KtReturnExpression
        if (returnStatements.any { it != lastReturn }) {
            val message = RefactoringBundle.getCannotRefactorMessage(
                if (returnStatements.size > 1)
                    KotlinBundle.message("error.text.inline.function.is.not.supported.for.functions.with.multiple.return.statements")
                else
                    KotlinBundle.message("error.text.inline.function.is.not.supported.for.functions.with.return.statements.not.at.the.end.of.the.body")
            )

            CommonRefactoringUtil.showErrorHint(
                declaration.project,
                editor,
                message,
                KotlinBundle.message("title.inline.function"),
                null
            )

            return null
        }

        return builder.prepareCodeToInline(
            lastReturn?.returnedExpression,
            statements.dropLast(returnStatements.size),
            ::analyzeExpressionInContext,
            reformat = true,
            declaration,
        )
    } else {
        return builder.prepareCodeToInline(
            bodyCopy,
            emptyList(),
            ::analyzeExpressionInContext,
            reformat = true,
            declaration,
        )
    }
}

internal fun Editor.findSimpleNameReference(): KtSimpleNameReference? =
    when (val reference = TargetElementUtil.findReference(this, caretModel.offset)) {
        is KtSimpleNameReference -> reference
        is PsiMultiReference -> reference.references.firstIsInstanceOrNull()
        else -> null
    }

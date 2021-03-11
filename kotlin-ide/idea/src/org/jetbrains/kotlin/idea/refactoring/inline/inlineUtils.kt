/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInliner.CodeToInline
import org.jetbrains.kotlin.idea.codeInliner.CodeToInlineBuilder
import org.jetbrains.kotlin.idea.intentions.ConvertReferenceToLambdaIntention
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal fun buildCodeToInline(
    declaration: KtDeclaration,
    bodyOrInitializer: KtExpression,
    isBlockBody: Boolean,
    editor: Editor?,
    fallbackToSuperCall: Boolean,
): CodeToInline? {
    val descriptor = declaration.unsafeResolveToDescriptor()
    val builder = CodeToInlineBuilder(
        targetCallable = descriptor as CallableDescriptor,
        resolutionFacade = declaration.getResolutionFacade(),
        originalDeclaration = declaration,
        fallbackToSuperCall = fallbackToSuperCall,
    )

    val expressionMapper: (KtExpression) -> Pair<KtExpression?, List<KtExpression>>? = if (isBlockBody) {
        fun(bodyOrInitializer: KtExpression): Pair<KtExpression?, List<KtExpression>>? {
            bodyOrInitializer as KtBlockExpression
            val statements = bodyOrInitializer.statements

            val returnStatements = bodyOrInitializer.collectDescendantsOfType<KtReturnExpression> {
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


            return lastReturn?.returnedExpression to statements.dropLast(returnStatements.size)
        }
    } else {
        { it to emptyList() }
    }

    return builder.prepareCodeToInlineWithAdvancedResolution(
        bodyOrExpression = bodyOrInitializer,
        expressionMapper = expressionMapper,
    )
}

internal fun Editor.findSimpleNameReference(): PsiReference? {
    val reference = TargetElementUtil.findReference(this, caretModel.offset) ?: return null
    return when {
        reference.element.language != KotlinLanguage.INSTANCE -> reference
        reference is KtSimpleNameReference -> reference
        reference is PsiMultiReference -> reference.references.firstIsInstanceOrNull<KtSimpleNameReference>()
        else -> null
    }
}

fun findCallableConflictForUsage(usage: PsiElement): @NlsContexts.DialogMessage String? {
    val usageParent = usage.parent as? KtCallableReferenceExpression ?: return null
    if (usageParent.callableReference != usage) return null
    if (ConvertReferenceToLambdaIntention.isApplicableTo(usageParent)) return null
    return KotlinBundle.message("text.reference.cannot.be.converted.to.a.lambda")
}

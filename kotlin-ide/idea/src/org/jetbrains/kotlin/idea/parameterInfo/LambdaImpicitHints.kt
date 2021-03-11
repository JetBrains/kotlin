/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiComment
import com.intellij.psi.TokenType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isUnit

fun provideLambdaImplicitHints(lambda: KtLambdaExpression): List<InlayInfo> {
    val lbrace = lambda.leftCurlyBrace
    if (!lbrace.isFollowedByNewLine()) {
        return emptyList()
    }
    val bindingContext = lambda.analyze(BodyResolveMode.PARTIAL)
    val functionDescriptor = bindingContext[BindingContext.FUNCTION, lambda.functionLiteral] ?: return emptyList()

    val implicitReceiver = functionDescriptor.extensionReceiverParameter
    if (implicitReceiver != null) {
        val text = buildString {
            append(TYPE_INFO_PREFIX)
            append("this: ")
            append(getInlayHintsTypeRenderer(bindingContext, lambda).renderType(implicitReceiver.type))
        }
        return listOf(InlayInfo(text, lbrace.textRange.endOffset))
    }

    val singleParameter = functionDescriptor.valueParameters.singleOrNull()
    if (singleParameter != null && bindingContext[BindingContext.AUTO_CREATED_IT, singleParameter] == true) {
        val type = singleParameter.type
        if (type.isUnit()) return emptyList()
        val text = buildString {
            append(TYPE_INFO_PREFIX)
            append("it: ")
            append(getInlayHintsTypeRenderer(bindingContext, lambda).renderType(type))
        }
        return listOf(InlayInfo(text, lbrace.textRange.endOffset))
    }
    return emptyList()
}

private fun ASTNode.isFollowedByNewLine(): Boolean {
    for (sibling in siblings()) {
        if (sibling.elementType != TokenType.WHITE_SPACE && sibling.psi !is PsiComment) {
            return false
        }
        if (sibling.elementType == TokenType.WHITE_SPACE && sibling.textContains('\n')) {
            return true
        }
    }
    return false
}

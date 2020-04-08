/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.google.common.collect.Lists
import com.intellij.psi.MultiRangeReference
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.core.canMoveLambdaOutsideParentheses
import org.jetbrains.kotlin.idea.core.moveFunctionLiteralOutsideParentheses
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.buildExpression
import org.jetbrains.kotlin.psi.psiUtil.getPossiblyQualifiedCallExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.INDEXED_LVALUE_GET
import org.jetbrains.kotlin.resolve.BindingContext.INDEXED_LVALUE_SET
import org.jetbrains.kotlin.util.OperatorNameConventions

class KtArrayAccessReference(
    expression: KtArrayAccessExpression
) : KtSimpleReference<KtArrayAccessExpression>(expression), MultiRangeReference {
    override val resolvesByNames: Collection<Name>
        get() = NAMES

    override fun getRangeInElement() = element.textRange.shiftRight(-element.textOffset)

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val getFunctionDescriptor = context[INDEXED_LVALUE_GET, expression]?.candidateDescriptor
        val setFunctionDescriptor = context[INDEXED_LVALUE_SET, expression]?.candidateDescriptor
        return listOfNotNull(getFunctionDescriptor, setFunctionDescriptor)
    }

    private fun getBracketRange(bracketToken: KtToken) =
        expression.indicesNode.node.findChildByType(bracketToken)?.textRange?.shiftRight(-expression.textOffset)

    override fun getRanges() = listOfNotNull(getBracketRange(KtTokens.LBRACKET), getBracketRange(KtTokens.RBRACKET))

    override fun canRename() = true

    override fun handleElementRename(newElementName: String): PsiElement? {
        val arrayAccessExpression = expression
        if (OperatorNameConventions.INVOKE.asString() == newElementName) {
            val replacement = KtPsiFactory(arrayAccessExpression.project).buildExpression {
                val arrayExpression = arrayAccessExpression.arrayExpression
                if (arrayExpression is KtQualifiedExpression) {
                    appendExpression(arrayExpression.receiverExpression)
                    appendFixedText(arrayExpression.operationSign.value)
                    appendExpression(arrayExpression.selectorExpression)
                } else {
                    appendExpression(arrayExpression)
                }

                appendFixedText("(")
                appendExpressions(arrayAccessExpression.indexExpressions, ",")
                appendFixedText(")")
            }
            val fullCallExpression = arrayAccessExpression.replaced(replacement)
            val callExpression = fullCallExpression.getPossiblyQualifiedCallExpression()
            if (callExpression != null && callExpression.canMoveLambdaOutsideParentheses()) {
                callExpression.moveFunctionLiteralOutsideParentheses()
            }
            return fullCallExpression
        }

        return this.renameImplicitConventionalCall(newElementName)
    }

    companion object {
        private val NAMES = Lists.newArrayList(OperatorNameConventions.GET, OperatorNameConventions.SET)
    }
}

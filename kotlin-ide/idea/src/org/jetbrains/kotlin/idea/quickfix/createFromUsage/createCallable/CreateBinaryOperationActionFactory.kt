/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import java.util.*

object CreateBinaryOperationActionFactory : CreateCallableMemberFromUsageFactory<KtBinaryExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtBinaryExpression? {
        return diagnostic.psiElement.parent as? KtBinaryExpression
    }

    override fun createCallableInfo(element: KtBinaryExpression, diagnostic: Diagnostic): CallableInfo? {
        val token = element.operationToken as KtToken
        val operationName = when (token) {
            KtTokens.IDENTIFIER -> element.operationReference.getReferencedName()
            else -> OperatorConventions.getNameForOperationSymbol(token, false, true)?.asString()
        } ?: return null
        val inOperation = token in OperatorConventions.IN_OPERATIONS
        val comparisonOperation = token in OperatorConventions.COMPARISON_OPERATIONS

        val leftExpr = element.left ?: return null
        val rightExpr = element.right ?: return null

        val receiverExpr = if (inOperation) rightExpr else leftExpr
        val argumentExpr = if (inOperation) leftExpr else rightExpr

        val builtIns = element.builtIns
        val receiverType = TypeInfo(receiverExpr, Variance.IN_VARIANCE)
        val returnType = when {
            inOperation -> TypeInfo.ByType(builtIns.booleanType, Variance.INVARIANT).noSubstitutions()
            comparisonOperation -> TypeInfo.ByType(builtIns.intType, Variance.INVARIANT).noSubstitutions()
            else -> TypeInfo(element, Variance.OUT_VARIANCE)
        }
        val parameters = Collections.singletonList(ParameterInfo(TypeInfo(argumentExpr, Variance.IN_VARIANCE)))
        val isOperator = token != KtTokens.IDENTIFIER
        return FunctionInfo(
            operationName,
            receiverType,
            returnType,
            parameterInfos = parameters,
            modifierList = KtPsiFactory(element).createModifierList(if (isOperator) KtTokens.OPERATOR_KEYWORD else KtTokens.INFIX_KEYWORD)
        )
    }
}

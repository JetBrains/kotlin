/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.components.service
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.utils.addToStdlib.constant

interface ReadWriteAccessChecker {
    fun readWriteAccessWithFullExpressionByResolve(assignment: KtBinaryExpression): Pair<ReferenceAccess, KtExpression>? = null

    fun readWriteAccessWithFullExpression(
        targetExpression: KtExpression,
        useResolveForReadWrite: Boolean
    ): Pair<ReferenceAccess, KtExpression> {
        var expression = targetExpression.getQualifiedExpressionForSelectorOrThis()
        loop@ while (true) {
            when (val parent = expression.parent) {
                is KtParenthesizedExpression, is KtAnnotatedExpression, is KtLabeledExpression -> expression = parent as KtExpression
                else -> break@loop
            }
        }

        val assignment = expression.getAssignmentByLHS()
        if (assignment != null) {
            when (assignment.operationToken) {
                KtTokens.EQ -> return ReferenceAccess.WRITE to assignment

                else -> {
                    return (
                            if (useResolveForReadWrite) readWriteAccessWithFullExpressionByResolve(assignment)
                            else null
                            ) ?: ReferenceAccess.READ_WRITE to assignment
                }
            }
        }

        val unaryExpression = expression.parent as? KtUnaryExpression
        return if (unaryExpression != null && unaryExpression.operationToken in constant { setOf(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS) })
            ReferenceAccess.READ_WRITE to unaryExpression
        else
            ReferenceAccess.READ to expression
    }

    companion object {
        fun getInstance(): ReadWriteAccessChecker = service()
    }
}

enum class ReferenceAccess(val isRead: Boolean, val isWrite: Boolean) {
    READ(true, false), WRITE(false, true), READ_WRITE(true, true)
}

fun KtExpression.readWriteAccess(useResolveForReadWrite: Boolean) =
    readWriteAccessWithFullExpression(useResolveForReadWrite).first

fun KtExpression.readWriteAccessWithFullExpression(useResolveForReadWrite: Boolean): Pair<ReferenceAccess, KtExpression> =
    ReadWriteAccessChecker.getInstance().readWriteAccessWithFullExpression(this, useResolveForReadWrite)



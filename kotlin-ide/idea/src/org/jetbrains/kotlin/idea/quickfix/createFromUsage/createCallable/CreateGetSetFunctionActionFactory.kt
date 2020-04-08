/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtContainerNode
import org.jetbrains.kotlin.psi.KtPsiUtil

abstract class CreateGetSetFunctionActionFactory(private val isGet: Boolean) :
    CreateCallableMemberFromUsageFactory<KtArrayAccessExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtArrayAccessExpression? {
        return when (diagnostic.factory) {
            Errors.NO_GET_METHOD -> if (isGet) Errors.NO_GET_METHOD.cast(diagnostic).psiElement else null
            Errors.NO_SET_METHOD -> if (!isGet) Errors.NO_SET_METHOD.cast(diagnostic).psiElement else null
            in Errors.TYPE_MISMATCH_ERRORS -> {
                val indicesNode = diagnostic.psiElement.parent as? KtContainerNode ?: return null
                if (indicesNode.node.elementType != KtNodeTypes.INDICES) return null
                val arrayAccess = indicesNode.parent as? KtArrayAccessExpression ?: return null
                val parent = arrayAccess.parent
                val isAssignmentLHS = KtPsiUtil.isAssignment(parent) && (parent as KtBinaryExpression).left == arrayAccess
                if (isAssignmentLHS == isGet) return null

                arrayAccess
            }
            else -> throw IllegalArgumentException("Unexpected diagnostic: ${diagnostic.factory.name}")
        }
    }
}

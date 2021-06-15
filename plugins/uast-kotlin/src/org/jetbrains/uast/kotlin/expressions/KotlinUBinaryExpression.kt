/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastBinaryOperator

class KotlinUBinaryExpression(
    override val sourcePsi: KtBinaryExpression,
    givenParent: UElement?
) : KotlinAbstractUBinaryExpression(sourcePsi, givenParent) {

    override fun handleBitwiseOperators(): UastBinaryOperator {
        val other = UastBinaryOperator.OTHER
        val ref = sourcePsi.operationReference
        val resolvedCall = sourcePsi.operationReference.getResolvedCall(ref.analyze()) ?: return other
        val resultingDescriptor = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return other
        val applicableOperator = BITWISE_OPERATORS[resultingDescriptor.name.asString()] ?: return other

        val containingClass = resultingDescriptor.containingDeclaration as? ClassDescriptor ?: return other
        return if (containingClass.typeConstructor.supertypes.any {
                it.constructor.declarationDescriptor?.fqNameSafe?.asString() == "kotlin.Number"
            }) applicableOperator else other
    }
}

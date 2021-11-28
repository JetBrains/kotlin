/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.translate.intrinsic.operation

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator
import org.jetbrains.kotlin.js.backend.ast.JsNullLiteral
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.TopLevelFIF
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.isNegatedOperation
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.isDynamic

object EqualsBOIF : BinaryOperationIntrinsicFactory {
    override fun getSupportTokens() = OperatorConventions.EQUALS_OPERATIONS!!

    private val equalsNullIntrinsic = BinaryOperationIntrinsic { expression, left, right, context ->
        val (subject, ktSubject) = if (right is JsNullLiteral) Pair(left, expression.left!!) else Pair(right, expression.right!!)
        TranslationUtils.nullCheck(ktSubject, subject, context, isNegatedOperation(expression))
    }

    private val kotlinEqualsIntrinsic = BinaryOperationIntrinsic { expression, left, right, context ->
        val coercedLeft = TranslationUtils.coerce(context, left, context.currentModule.builtIns.anyType)
        val coercedRight = TranslationUtils.coerce(context, right, context.currentModule.builtIns.anyType)
        val result = TopLevelFIF.KOTLIN_EQUALS.apply(coercedLeft, listOf(coercedRight), context)
        if (isNegatedOperation(expression)) JsAstUtils.not(result) else result
    }

    private val refEqSelector: OperatorSelector = { if (isNegatedOperation(it)) JsBinaryOperator.REF_NEQ else JsBinaryOperator.REF_EQ }

    private val eqSelector: OperatorSelector = { if (isNegatedOperation(it)) JsBinaryOperator.NEQ else JsBinaryOperator.EQ }

    private val refEqIntrinsic = binaryIntrinsic(operator = refEqSelector)

    private val eqIntrinsic = binaryIntrinsic(operator = eqSelector)

    private fun primitiveTypes(
        leftKotlinType: KotlinType, rightKotlinType: KotlinType
    ): BinaryOperationIntrinsic {

        fun <T> select(number: T, long: T, bool: T, char: T): (KotlinType) -> T = {
            when {
                KotlinBuiltIns.isLongOrNullableLong(it) -> long
                KotlinBuiltIns.isBooleanOrNullableBoolean(it) -> bool
                KotlinBuiltIns.isCharOrNullableChar(it) -> char
                else -> number
            }
        }

        val eq = binaryIntrinsic(coerceTo(leftKotlinType), coerceTo(rightKotlinType), eqSelector)

        val refEq = binaryIntrinsic(coerceTo(leftKotlinType), coerceTo(rightKotlinType), refEqSelector)

        // Used for number to number comparison
        val default = { leftNullable: Boolean, rightNullable: Boolean ->
            if (leftNullable && rightNullable) eq else refEq
        }

        // Used to compare Boolean with number types and Long. Kotlin.equals handles cases like 0: Int? == false: Boolean?
        val bool = { leftNullable: Boolean, rightNullable: Boolean ->
            if (leftNullable && rightNullable) kotlinEqualsIntrinsic else refEq
        }

        // Used to compare Long with number types.
        val allEq = { _: Boolean, _: Boolean -> eq }

        // Used to compare Char with other primitive types and Long with Long
        val allKEq = { _: Boolean, _: Boolean -> kotlinEqualsIntrinsic }

        return select(
            select(default, allEq, bool, allKEq),
            select(allEq, allKEq, bool, allKEq),
            select(bool, bool, default, allKEq),
            select(allKEq, allKEq, allKEq, default)
        )(leftKotlinType)(rightKotlinType)(TypeUtils.isNullableType(leftKotlinType), TypeUtils.isNullableType(rightKotlinType))
    }

    override fun getIntrinsic(descriptor: FunctionDescriptor, leftType: KotlinType?, rightType: KotlinType?): BinaryOperationIntrinsic? =
        when {
            leftType == null || rightType == null -> null

            isEnumEqualsIntrinsicApplicable(descriptor, leftType, rightType) -> refEqIntrinsic

            KotlinBuiltIns.isBuiltIn(descriptor) || TopLevelFIF.EQUALS_IN_ANY.test(descriptor) -> BinaryOperationIntrinsic { expression, left, right, context ->
                when {
                    left is JsNullLiteral || right is JsNullLiteral -> equalsNullIntrinsic
                    leftType.primitive && rightType.primitive -> primitiveTypes(leftType, rightType)
                    expression.appliedToDynamic(context) -> eqIntrinsic
                    else -> kotlinEqualsIntrinsic
                }(expression, left, right, context)
            }

            else -> null
        }

    private fun KtBinaryExpression.appliedToDynamic(context: TranslationContext) =
        getResolvedCall(context.bindingContext())?.dispatchReceiver?.type?.isDynamic() ?: false

    private val KotlinType.primitive
        get() = KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType(this)

    private fun isEnumEqualsIntrinsicApplicable(descriptor: FunctionDescriptor, leftType: KotlinType, rightType: KotlinType): Boolean {
        return DescriptorUtils.isEnumClass(descriptor.containingDeclaration) &&
                !TypeUtils.isNullableType(leftType) && !TypeUtils.isNullableType(rightType)
    }
}

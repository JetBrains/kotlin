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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsIntLiteral
import org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern
import org.jetbrains.kotlin.js.translate.operation.OperatorTable
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getOperationToken
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.identity

object CompareToBOIF : BinaryOperationIntrinsicFactory {
    override fun getSupportTokens(): Set<KtSingleValueToken> = OperatorConventions.COMPARISON_OPERATIONS

    // toLeft(L, R) OP toRight(L, R)
    private fun intrinsic(
        toLeft: (JsExpression, JsExpression) -> JsExpression,
        toRight: (JsExpression, JsExpression) -> JsExpression
    ): BinaryOperationIntrinsic = { expression, left, right, _ ->
        val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
        JsBinaryOperation(operator, toLeft(left, right), toRight(left, right))
    }

    // toLeft(L) OP toRight(R)
    private fun primitiveIntrinsic(
        toLeft: (JsExpression) -> JsExpression,
        toRight: (JsExpression) -> JsExpression
    ): BinaryOperationIntrinsic = intrinsic({ l, _ -> toLeft(l) }, { _, r -> toRight(r) })

    // toLeft(L).compareTo(toRight(R)) OP 0
    private fun compareToIntrinsic(
        toLeft: (JsExpression) -> JsExpression,
        toRight: (JsExpression) -> JsExpression
    ): BinaryOperationIntrinsic = intrinsic({ l, r -> compareForObject(toLeft(l), toRight(r)) }, { _, _ -> JsIntLiteral(0) })

    private fun unboxCharIfNeeded(type: KotlinType): (JsExpression) -> JsExpression = { e ->
        if (KotlinBuiltIns.isCharOrNullableChar(type)) {
            charToInt(e)
        } else e
    }

    // TODO Couldn't Long be converted to Number in all cases except Long.compareTo(Long)?
    private val patterns = listOf(
        pattern("Double|Float.compareTo(Long)") to primitiveIntrinsic(identity(), ::longToNumber),
        pattern("Long.compareTo(Float|Double)") to primitiveIntrinsic(::longToNumber, identity()),
        pattern("Int|Short|Byte.compareTo(Long)") to compareToIntrinsic(::longFromInt, identity()),
        pattern("Long.compareTo(Int|Short|Byte)") to compareToIntrinsic(identity(), ::longFromInt),
        pattern("Char.compareTo(Long)") to compareToIntrinsic({ longFromInt(charToInt(it)) }, identity()),
        pattern("Long.compareTo(Char)") to compareToIntrinsic(identity(), { longFromInt(JsAstUtils.charToInt(it)) }),
        pattern("Long.compareTo(Long)") to compareToIntrinsic(identity(), identity())
    )

    override fun getIntrinsic(descriptor: FunctionDescriptor, leftType: KotlinType?, rightType: KotlinType?): BinaryOperationIntrinsic? {
        if (descriptor.isDynamic()) return primitiveIntrinsic(identity(), identity())

        if (leftType == null || rightType == null || !KotlinBuiltIns.isBuiltIn(descriptor)) return null

        patterns.forEach { (p, i) -> if (p.test(descriptor)) return i }

        // Types may be nullable if properIeeeComparisons are switched off, e.g. fun foo(a: Double?) = a != null && a < 0.0
        return if (isPrimitiveTypeOrNullablePrimitiveType(leftType) && isPrimitiveTypeOrNullablePrimitiveType(rightType)) {
            primitiveIntrinsic(unboxCharIfNeeded(leftType), unboxCharIfNeeded(rightType))
        } else {
            intrinsic({ l, r -> compareTo(l, r) }, { _, _ -> JsIntLiteral(0) })
        }
    }
}

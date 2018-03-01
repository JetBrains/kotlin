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
import org.jetbrains.kotlin.js.backend.ast.JsIntLiteral
import org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.identity

object CompareToBOIF : BinaryOperationIntrinsicFactory {
    override fun getSupportTokens(): Set<KtSingleValueToken> = OperatorConventions.COMPARISON_OPERATIONS

    private val patterns = listOf(
        pattern("Int|Short|Byte|Float|Double.compareTo(Long)") to binaryIntrinsic(toRight = { r, _ -> longToNumber(r) }),
        pattern("Long.compareTo(Int|Short|Byte|Float|Double)") to binaryIntrinsic(toLeft = { l, _ -> longToNumber(l) }),
        // L.compareTo(R) OP 0
        pattern("Long.compareTo(Long)") to intrinsic({ l, r, _ -> compareForObject(l, r) }, { _, _, _-> JsIntLiteral(0) })
    )

    override fun getIntrinsic(descriptor: FunctionDescriptor, leftType: KotlinType?, rightType: KotlinType?): BinaryOperationIntrinsic? {
        if (descriptor.isDynamic()) return binaryIntrinsic()

        if (leftType == null || rightType == null || !KotlinBuiltIns.isBuiltIn(descriptor)) return null

        patterns.forEach { (p, i) -> if (p.test(descriptor)) return i }

        // Types may be nullable if properIeeeComparisons are switched off, e.g. fun foo(a: Double?) = a != null && a < 0.0
        return if (isPrimitiveTypeOrNullablePrimitiveType(leftType) && isPrimitiveTypeOrNullablePrimitiveType(rightType)) {
            binaryIntrinsic(coerceTo(leftType), coerceTo(rightType))
        } else {
            // Kotlin.compareTo(L, R) OP 0
            intrinsic({ l, r, _ -> compareTo(l, r) }, { _, _, _ -> JsIntLiteral(0) })
        }
    }
}

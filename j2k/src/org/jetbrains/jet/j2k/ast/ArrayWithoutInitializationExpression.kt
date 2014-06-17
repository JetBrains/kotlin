/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.CommentConverter

open class ArrayWithoutInitializationExpression(val `type`: ArrayType, val expressions: List<Expression>) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter): String {
        fun getConstructorName(`type`: ArrayType, hasInit: Boolean): String {
            return when (`type`.elementType) {
                is PrimitiveType ->
                    `type`.toNotNullType().toKotlin(commentConverter)
                is ArrayType ->
                    if (hasInit)
                        `type`.toNotNullType().toKotlin(commentConverter)
                    else
                        "arrayOfNulls<" + `type`.elementType.toKotlin(commentConverter) + ">"
                else ->
                    "arrayOfNulls<" + `type`.elementType.toKotlin(commentConverter) + ">"
            }
        }

        fun oneDim(`type`: ArrayType, size: Expression, init: String = ""): String {
            return getConstructorName(`type`, !init.isEmpty()) + "(" + size.toKotlin(commentConverter) + init.withPrefix(", ") + ")"
        }

        fun constructInnerType(hostType: ArrayType, expressions: List<Expression>): String {
            if (expressions.size() == 1) {
                return oneDim(hostType, expressions[0])
            }

            val innerType = hostType.elementType
            if (expressions.size() > 1 && innerType is ArrayType) {
                return oneDim(hostType, expressions[0], "{" + constructInnerType(innerType, expressions.subList(1, expressions.size())) + "}")
            }

            return getConstructorName(hostType, expressions.size() != 0)
        }

        return constructInnerType(`type`, expressions)
    }
}

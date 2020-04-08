/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k.ast

import org.jetbrains.kotlin.j2k.CodeBuilder

class ArrayWithoutInitializationExpression(val type: ArrayType, val expressions: List<Expression>) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        fun appendConstructorName(type: ArrayType, hasInit: Boolean): CodeBuilder = when (type.elementType) {
            is PrimitiveType -> builder.append(type.toNotNullType())

            is ArrayType ->
                if (hasInit) {
                    builder.append(type.toNotNullType())
                }
                else {
                    builder.append("arrayOfNulls<").append(type.elementType).append(">")
                }

            else -> builder.append("arrayOfNulls<").append(type.elementType).append(">")
        }

        fun oneDim(type: ArrayType, size: Expression, init: (() -> Unit)? = null): CodeBuilder {
            appendConstructorName(type, init != null).append("(").append(size)
            if (init != null) {
                builder.append(", ")
                init()
            }
            return builder.append(")")
        }

        fun constructInnerType(hostType: ArrayType, expressions: List<Expression>): CodeBuilder {
            if (expressions.size == 1) {
                return oneDim(hostType, expressions[0])
            }

            val innerType = hostType.elementType
            if (expressions.size > 1 && innerType is ArrayType) {
                return oneDim(hostType, expressions[0], {
                    builder.append("{")
                    constructInnerType(innerType, expressions.subList(1, expressions.size))
                    builder.append("}")
                })
            }

            return appendConstructorName(hostType, expressions.isNotEmpty())
        }

        constructInnerType(type, expressions)
    }
}

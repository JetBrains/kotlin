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

class ArrayAccessExpression(val expression: Expression, val index: Expression, val lvalue: Boolean) : Expression() {
    override fun toKotlin() = operandToKotlin(expression) +
            (if (!lvalue && expression.isNullable) "!!" else "") +
            "[" + index.toKotlin() + "]"
}

class AssignmentExpression(val left: Expression, val right: Expression, val op: String) : Expression() {
    override fun toKotlin() = operandToKotlin(left) + " " + op + " " + operandToKotlin(right)
}

class BangBangExpression(val expr: Expression) : Expression() {
    override fun toKotlin() = operandToKotlin(expr) + "!!"
}

class BinaryExpression(val left: Expression, val right: Expression, val op: String) : Expression() {
    override fun toKotlin() = operandToKotlin(left, false) + " " + op + " " + operandToKotlin(right, true)
}

class IsOperator(val expression: Expression, val typeElement: TypeElement) : Expression() {
    override fun toKotlin() = operandToKotlin(expression) + " is " + typeElement.toKotlinNotNull()
}

class TypeCastExpression(val `type`: Type, val expression: Expression) : Expression() {
    override fun toKotlin() = operandToKotlin(expression) + " as " + `type`.toKotlin()
}

class LiteralExpression(val literalText: String) : Expression() {
    override fun toKotlin() = literalText
}

class ParenthesizedExpression(val expression: Expression) : Expression() {
    override fun toKotlin() = "(" + expression.toKotlin() + ")"
}

class PrefixOperator(val op: String, val expression: Expression) : Expression() {
    override fun toKotlin() = op + operandToKotlin(expression)

    override val isNullable: Boolean
        get() = expression.isNullable
}

class PostfixOperator(val op: String, val expression: Expression) : Expression() {
    override fun toKotlin() = operandToKotlin(expression) + op
}

class ThisExpression(val identifier: Identifier) : Expression() {
    override fun toKotlin() = "this" + identifier.withPrefix("@")
}

class SuperExpression(val identifier: Identifier) : Expression() {
    override fun toKotlin() = "super" + identifier.withPrefix("@")
}

class QualifiedExpression(val expression: Expression, val identifier: Expression) : Expression() {
    override val isNullable: Boolean
        get() {
            if (!expression.isEmpty && expression.isNullable) return true
            return identifier.isNullable
        }

    override fun toKotlin(): String {
        if (!expression.isEmpty) {
            return operandToKotlin(expression) + (if (expression.isNullable) "?." else ".") + identifier.toKotlin()
        }

        return identifier.toKotlin()
    }
}

class PolyadicExpression(val expressions: List<Expression>, val token: String) : Expression() {
    override fun toKotlin(): String {
        val expressionsWithConversions = expressions.map { it.toKotlin() }
        return expressionsWithConversions.makeString(" " + token + " ")
    }
}

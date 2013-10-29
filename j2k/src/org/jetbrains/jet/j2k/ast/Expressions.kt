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

import org.jetbrains.jet.j2k.ast.types.Type

public open class ArrayAccessExpression(val expression: Expression, val index: Expression, val lvalue: Boolean) : Expression() {
    override fun toKotlin() = expression.toKotlin() +
    (if (!lvalue && expression.isNullable()) "!!" else "") +
    "[" + index.toKotlin() + "]"
}

public open class AssignmentExpression(val left: Expression, val right: Expression, val op: String) : Expression() {
    override fun toKotlin() = left.toKotlin() + " " + op + " " + right.toKotlin()
}

public class BangBangExpression(val expr: Expression) : Expression() {
    override fun toKotlin() = expr.toKotlin() + "!!"
}

public open class BinaryExpression(val left: Expression, val right: Expression, val op: String) : Expression() {
    override fun toKotlin() = left.toKotlin() + " " + op + " " + right.toKotlin()
}

public open class ClassObjectAccessExpression(val typeElement: TypeElement) : Expression() {
    override fun toKotlin() = "javaClass<" + typeElement.toKotlinNotNull() + ">()"
}

public open class IsOperator(val expression: Expression, val typeElement: TypeElement) : Expression() {
    override fun toKotlin() = expression.toKotlin() + " is " + typeElement.toKotlinNotNull()
}

public open class TypeCastExpression(val `type`: Type, val expression: Expression) : Expression() {
    override fun toKotlin() = "(" + expression.toKotlin() + " as " + `type`.toKotlin() + ")"
}

public open class LiteralExpression(val literalText: String) : Expression() {
    override fun toKotlin() = literalText
}

public open class ParenthesizedExpression(val expression: Expression) : Expression() {
    override fun toKotlin() = "(" + expression.toKotlin() + ")"
}

public open class PrefixOperator(val op: String, val expression: Expression) : Expression() {
    override fun toKotlin() = op + expression.toKotlin()
    override fun isNullable() = expression.isNullable()
}

public open class PostfixOperator(val op: String, val expression: Expression) : Expression() {
    override fun toKotlin() = expression.toKotlin() + op
}

public open class ThisExpression(val identifier: Identifier) : Expression() {
    override fun toKotlin() = "this" + identifier.withPrefix("@")
}

public open class SuperExpression(val identifier: Identifier) : Expression() {
    override fun toKotlin() = "super" + identifier.withPrefix("@")
}

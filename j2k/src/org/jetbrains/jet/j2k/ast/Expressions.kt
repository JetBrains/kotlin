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

import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import org.jetbrains.jet.j2k.CommentConverter

class ArrayAccessExpression(val expression: Expression, val index: Expression, val lvalue: Boolean) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = operandToKotlin(expression, commentConverter) +
            (if (!lvalue && expression.isNullable) "!!" else "") +
            "[" + index.toKotlin(commentConverter) + "]"
}

class AssignmentExpression(val left: Expression, val right: Expression, val op: String) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = operandToKotlin(left, commentConverter) + " " + op + " " + operandToKotlin(right, commentConverter)
}

class BangBangExpression(val expr: Expression) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = operandToKotlin(expr, commentConverter) + "!!"
}

class BinaryExpression(val left: Expression, val right: Expression, val op: String) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = operandToKotlin(left, commentConverter, false) + " " + op + " " + operandToKotlin(right, commentConverter, true)
}

class IsOperator(val expression: Expression, val typeElement: TypeElement) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = operandToKotlin(expression, commentConverter) + " is " + typeElement.`type`.toNotNullType().toKotlin(commentConverter)
}

class TypeCastExpression(val `type`: Type, val expression: Expression) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = operandToKotlin(expression, commentConverter) + " as " + `type`.toKotlin(commentConverter)
}

class LiteralExpression(val literalText: String) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = literalText
}

class ParenthesizedExpression(val expression: Expression) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = "(" + expression.toKotlin(commentConverter) + ")"
}

class PrefixOperator(val op: String, val expression: Expression) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = op + operandToKotlin(expression, commentConverter)

    override val isNullable: Boolean
        get() = expression.isNullable
}

class PostfixOperator(val op: String, val expression: Expression) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = operandToKotlin(expression, commentConverter) + op
}

class ThisExpression(val identifier: Identifier) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = "this" + identifier.withPrefix("@", commentConverter)
}

class SuperExpression(val identifier: Identifier) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = "super" + identifier.withPrefix("@", commentConverter)
}

class QualifiedExpression(val qualifier: Expression, val identifier: Expression) : Expression() {
    override val isNullable: Boolean
        get() = identifier.isNullable

    override fun toKotlinImpl(commentConverter: CommentConverter): String {
        if (!qualifier.isEmpty) {
            return operandToKotlin(qualifier, commentConverter) + (if (qualifier.isNullable) "!!." else ".") + identifier.toKotlin(commentConverter)
        }

        return identifier.toKotlin(commentConverter)
    }
}

class PolyadicExpression(val expressions: List<Expression>, val token: String) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter): String {
        val expressionsWithConversions = expressions.map { it.toKotlin(commentConverter) }
        return expressionsWithConversions.makeString(" " + token + " ")
    }
}

class LambdaExpression(val arguments: String?, val statementList: StatementList) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter): String {
        val statementsText = statementList.toKotlin(commentConverter).trim()
        if (arguments != null) {
            val br = if (statementsText.indexOf('\n') < 0 && statementsText.indexOf('\r') < 0) " " else "\n"
            return "{ $arguments ->$br$statementsText }"
        }
        else {
            return "{ $statementsText }"
        }
    }
}

class StarExpression(val methodCall: MethodCallExpression) : Expression() {
    override fun toKotlinImpl(commentConverter: CommentConverter) = "*" + methodCall.toKotlin(commentConverter)
}

fun createArrayInitializerExpression(arrayType: ArrayType, initializers: List<Expression>, needExplicitType: Boolean) : MethodCallExpression {
    val elementType = arrayType.elementType
    val createArrayFunction = if (elementType is PrimitiveType)
            (elementType.toNotNullType().toKotlin(CommentConverter.Dummy) + "Array").decapitalize()
        else if (needExplicitType)
            arrayType.toNotNullType().toKotlin(CommentConverter.Dummy).decapitalize()
        else
            "array"

    val doubleOrFloatTypes = setOf("double", "float", "java.lang.double", "java.lang.float")
    val afterReplace = arrayType.toNotNullType().toKotlin(CommentConverter.Dummy).replace("Array", "").toLowerCase().replace(">", "").replace("<", "").replace("?", "")

    fun explicitConvertIfNeeded(initializer: Expression): Expression {
        if (doubleOrFloatTypes.contains(afterReplace)) {
            if (initializer is LiteralExpression) {
                if (!initializer.toKotlin(CommentConverter.Dummy).contains(".")) {
                    return LiteralExpression(initializer.literalText + ".0")
                }
            }
            else {
                val conversionFunction = when {
                    afterReplace.contains("double") -> OperatorConventions.DOUBLE.getIdentifier()
                    afterReplace.contains("float") -> OperatorConventions.FLOAT.getIdentifier()
                    else -> null
                }
                if (conversionFunction != null) {
                    return MethodCallExpression.buildNotNull(initializer, conversionFunction)
                }
            }
        }

        return initializer
    }

    return MethodCallExpression.buildNotNull(null, createArrayFunction, initializers.map { explicitConvertIfNeeded(it) })
}
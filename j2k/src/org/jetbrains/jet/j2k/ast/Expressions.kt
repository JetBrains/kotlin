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
import org.jetbrains.jet.j2k.CommentsAndSpaces

class ArrayAccessExpression(val expression: Expression, val index: Expression, val lvalue: Boolean) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = operandToKotlin(expression, commentsAndSpaces) +
            (if (!lvalue && expression.isNullable) "!!" else "") +
            "[" + index.toKotlin(commentsAndSpaces) + "]"
}

class AssignmentExpression(val left: Expression, val right: Expression, val op: String) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = operandToKotlin(left, commentsAndSpaces) + " " + op + " " + operandToKotlin(right, commentsAndSpaces)
}

class BangBangExpression(val expr: Expression) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = operandToKotlin(expr, commentsAndSpaces) + "!!"
}

class BinaryExpression(val left: Expression, val right: Expression, val op: String) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = operandToKotlin(left, commentsAndSpaces, false) + " " + op + " " + operandToKotlin(right, commentsAndSpaces, true)
}

class IsOperator(val expression: Expression, val typeElement: TypeElement) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = operandToKotlin(expression, commentsAndSpaces) + " is " + typeElement.`type`.toNotNullType().toKotlin(commentsAndSpaces)
}

class TypeCastExpression(val `type`: Type, val expression: Expression) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = operandToKotlin(expression, commentsAndSpaces) + " as " + `type`.toKotlin(commentsAndSpaces)
}

class LiteralExpression(val literalText: String) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = literalText
}

class ParenthesizedExpression(val expression: Expression) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "(" + expression.toKotlin(commentsAndSpaces) + ")"
}

class PrefixOperator(val op: String, val expression: Expression) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = op + operandToKotlin(expression, commentsAndSpaces)

    override val isNullable: Boolean
        get() = expression.isNullable
}

class PostfixOperator(val op: String, val expression: Expression) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = operandToKotlin(expression, commentsAndSpaces) + op
}

class ThisExpression(val identifier: Identifier) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "this" + identifier.withPrefix("@", commentsAndSpaces)
}

class SuperExpression(val identifier: Identifier) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "super" + identifier.withPrefix("@", commentsAndSpaces)
}

class QualifiedExpression(val qualifier: Expression, val identifier: Expression) : Expression() {
    override val isNullable: Boolean
        get() = identifier.isNullable

    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String {
        if (!qualifier.isEmpty) {
            return operandToKotlin(qualifier, commentsAndSpaces) + (if (qualifier.isNullable) "!!." else ".") + identifier.toKotlin(commentsAndSpaces)
        }

        return identifier.toKotlin(commentsAndSpaces)
    }
}

class PolyadicExpression(val expressions: List<Expression>, val token: String) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String {
        val expressionsWithConversions = expressions.map { it.toKotlin(commentsAndSpaces) }
        return expressionsWithConversions.makeString(" " + token + " ")
    }
}

class LambdaExpression(val arguments: String?, val block: Block) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String {
        val statementsText = block.statements.toKotlin(commentsAndSpaces, "\n").trim()
        val innerBody = if (arguments != null) {
            val br = if (statementsText.indexOf('\n') < 0 && statementsText.indexOf('\r') < 0) " " else "\n"
            "$arguments ->$br$statementsText"
        }
        else {
            statementsText
        }
        return block.lBrace.toKotlin(commentsAndSpaces) + " " + innerBody + " " + block.rBrace.toKotlin(commentsAndSpaces)
    }
}

class StarExpression(val methodCall: MethodCallExpression) : Expression() {
    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces) = "*" + methodCall.toKotlin(commentsAndSpaces)
}

fun createArrayInitializerExpression(arrayType: ArrayType, initializers: List<Expression>, needExplicitType: Boolean) : MethodCallExpression {
    val elementType = arrayType.elementType
    val createArrayFunction = if (elementType is PrimitiveType)
            (elementType.toNotNullType().toKotlin(CommentsAndSpaces.None) + "Array").decapitalize()
        else if (needExplicitType)
            arrayType.toNotNullType().toKotlin(CommentsAndSpaces.None).decapitalize()
        else
            "array"

    val doubleOrFloatTypes = setOf("double", "float", "java.lang.double", "java.lang.float")
    val afterReplace = arrayType.toNotNullType().toKotlin(CommentsAndSpaces.None).replace("Array", "").toLowerCase().replace(">", "").replace("<", "").replace("?", "")

    fun explicitConvertIfNeeded(initializer: Expression): Expression {
        if (doubleOrFloatTypes.contains(afterReplace)) {
            if (initializer is LiteralExpression) {
                if (!initializer.toKotlin(CommentsAndSpaces.None).contains(".")) {
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
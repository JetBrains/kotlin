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

import com.intellij.psi.JavaTokenType
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.j2k.CodeBuilder
import org.jetbrains.kotlin.j2k.append
import org.jetbrains.kotlin.lexer.KtTokens

class ArrayAccessExpression(val expression: Expression, val index: Expression, val lvalue: Boolean) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.appendOperand(this, expression)
        if (!lvalue && expression.isNullable) builder.append("!!")
        builder append "[" append index append "]"
    }
}

class AssignmentExpression(val left: Expression, val right: Expression, val op: Operator) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.appendOperand(this, left).append(" ").append(op).append(" ").appendOperand(this, right)
    }
}

class BangBangExpression(val expr: Expression) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.appendOperand(this, expr).append("!!")
    }

    companion object {
        fun surroundIfNullable(expression: Expression): Expression {
            return if (expression.isNullable)
                BangBangExpression(expression).assignNoPrototype()
            else
                expression
        }
    }
}

class BinaryExpression(val left: Expression, val right: Expression, val op: Operator) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.appendOperand(this, left, false).append(" ").append(op).append(" ").appendOperand(this, right, true)
    }
}

class IsOperator(val expression: Expression, val type: Type) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.appendOperand(this, expression).append(" is ").append(type)
    }
}

class TypeCastExpression(val type: Type, val expression: Expression) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.appendOperand(this, expression).append(" as ").append(type)
    }
}

class LiteralExpression(val literalText: String) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(literalText)
    }
}

class ParenthesizedExpression(val expression: Expression) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder append "(" append expression append ")"
    }
}

class PrefixExpression(val op: Operator, val expression: Expression) : Expression() {
    override fun generateCode(builder: CodeBuilder){
        builder.append(op).appendOperand(this, expression)
    }

    override val isNullable: Boolean
        get() = expression.isNullable
}

class PostfixExpression(val op: Operator, val expression: Expression) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.appendOperand(this, expression) append op
    }
}

class ThisExpression(val identifier: Identifier) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("this").appendWithPrefix(identifier, "@")
    }
}

class SuperExpression(val identifier: Identifier) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("super").appendWithPrefix(identifier, "@")
    }
}

class QualifiedExpression(val qualifier: Expression, val identifier: Expression) : Expression() {
    override val isNullable: Boolean
        get() = identifier.isNullable

    override fun generateCode(builder: CodeBuilder) {
        if (!qualifier.isEmpty) {
            builder.appendOperand(this, qualifier).append(if (qualifier.isNullable) "!!." else ".")
        }

        builder.append(identifier)
    }
}

class PolyadicExpression(val expressions: List<Expression>, val operators: List<Operator>) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        assert(expressions.size == operators.size + 1)
        for ((i, expression) in expressions.withIndex()) {
            builder.append(expression)
            if (i < operators.size) {
                builder.append(" ")
                builder.append(operators[i])
                builder.append(" ")
            }
        }
    }
}

open class Operator(val operatorType: IElementType): Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(asString(operatorType))
    }

    fun asString() = asString(operatorType)

    fun acceptLineBreakBefore(): Boolean {
        return when(operatorType) {
            JavaTokenType.ANDAND,
            JavaTokenType.OROR,
            JavaTokenType.PLUS,
            JavaTokenType.MINUS -> true
            else -> false
        }
    }

    private fun asString(tokenType: IElementType): String {
        return when(tokenType) {
            JavaTokenType.EQ -> "="
            JavaTokenType.EQEQ -> "=="
            JavaTokenType.NE -> "!="
            JavaTokenType.ANDAND -> "&&"
            JavaTokenType.OROR -> "||"
            JavaTokenType.GT -> ">"
            JavaTokenType.LT -> "<"
            JavaTokenType.GE -> ">="
            JavaTokenType.LE -> "<="
            JavaTokenType.EXCL -> "!"
            JavaTokenType.PLUS -> "+"
            JavaTokenType.MINUS -> "-"
            JavaTokenType.ASTERISK -> "*"
            JavaTokenType.DIV -> "/"
            JavaTokenType.PERC -> "%"
            JavaTokenType.PLUSEQ -> "+="
            JavaTokenType.MINUSEQ -> "-="
            JavaTokenType.ASTERISKEQ -> "*="
            JavaTokenType.DIVEQ -> "/="
            JavaTokenType.PERCEQ -> "%="
            JavaTokenType.GTGT -> "shr"
            JavaTokenType.LTLT -> "shl"
            JavaTokenType.XOR -> "xor"
            JavaTokenType.AND -> "and"
            JavaTokenType.OR -> "or"
            JavaTokenType.GTGTGT -> "ushr"
            JavaTokenType.GTGTEQ -> "shr"
            JavaTokenType.LTLTEQ -> "shl"
            JavaTokenType.XOREQ -> "xor"
            JavaTokenType.ANDEQ -> "and"
            JavaTokenType.OREQ -> "or"
            JavaTokenType.GTGTGTEQ -> "ushr"
            JavaTokenType.PLUSPLUS -> "++"
            JavaTokenType.MINUSMINUS -> "--"
            KtTokens.EQEQEQ -> "==="
            KtTokens.EXCLEQEQEQ -> "!=="
            else -> "" //System.out.println("UNSUPPORTED TOKEN TYPE: " + tokenType?.toString())
        }
    }

    companion object {
        val EQEQ = Operator(JavaTokenType.EQEQ).assignNoPrototype()
        val EQ = Operator(JavaTokenType.EQ).assignNoPrototype()
    }
}

class LambdaExpression(val parameterList: ParameterList?, val block: Block) : Expression() {
    init {
        assignPrototypesFrom(block)
    }

    override fun generateCode(builder: CodeBuilder) {
        builder append block.lBrace append " "

        if (parameterList != null && !parameterList.parameters.isEmpty()) {
            builder.append(parameterList)
                    .append("->")
                    .append(if (block.statements.size > 1) "\n" else " ")
                    .append(block.statements, "\n")
        }
        else {
            builder.append(block.statements, "\n")
        }

        builder append " " append block.rBrace
    }
}

class StarExpression(val operand: Expression) : Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append("*").appendOperand(this, operand)
    }
}

class RangeExpression(val start: Expression, val end: Expression): Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.appendOperand(this, start).append("..").appendOperand(this, end)
    }
}

class DownToExpression(val start: Expression, val end: Expression): Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.appendOperand(this, start).append(" downTo ").appendOperand(this, end)
    }
}

class ClassLiteralExpression(val type: Type): Expression() {
    override fun generateCode(builder: CodeBuilder) {
        builder.append(type).append("::class")
    }
}

fun createArrayInitializerExpression(arrayType: ArrayType, initializers: List<Expression>, needExplicitType: Boolean = true) : MethodCallExpression {
    val elementType = arrayType.elementType
    val createArrayFunction = if (elementType is PrimitiveType)
            (elementType.toNotNullType().canonicalCode() + "ArrayOf").decapitalize()
        else if (needExplicitType)
            "arrayOf<" + arrayType.elementType.canonicalCode() + ">"
        else
            "arrayOf"
    return MethodCallExpression.buildNotNull(null, createArrayFunction, initializers)
}

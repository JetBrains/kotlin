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

package org.jetbrains.kotlin.j2k.tree.impl

import com.intellij.psi.JavaTokenType
import com.intellij.psi.impl.source.tree.ElementType.OPERATION_BIT_SET
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.JKLiteralExpression.LiteralType.*
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType

class JKJavaFieldImpl(modifierList: JKModifierList, type: JKTypeElement, name: JKNameIdentifier, initializer: JKExpression) : JKJavaField,
    JKBranchElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaField(this, data)

    override var initializer: JKExpression by child(initializer)
    override var modifierList: JKModifierList by child(modifierList)
    override var type by child(type)
    override var name: JKNameIdentifier by child(name)
}

class JKJavaMethodImpl(
    modifierList: JKModifierList, returnType: JKTypeElement, name: JKNameIdentifier, parameters: List<JKParameter>, block: JKBlock
) : JKJavaMethod, JKBranchElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaMethod(this, data)

    override var modifierList: JKModifierList by child(modifierList)
    override var returnType: JKTypeElement by child(returnType)
    override var name: JKNameIdentifier by child(name)
    override var parameters: List<JKParameter> by children(parameters)
    override var block: JKBlock by child(block)

}

class JKJavaLiteralExpressionImpl(
    override val literal: String,
    override val type: JKLiteralExpression.LiteralType
) : JKJavaLiteralExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaLiteralExpression(this, data)

    init {
        require(type in setOf(STRING, CHAR, INT, LONG, FLOAT, DOUBLE))
    }
}

class JKJavaModifierImpl(override val type: JKJavaModifier.JavaModifierType) : JKJavaModifier, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaModifier(this, data)
}

class JKJavaOperatorImpl private constructor(val token: IElementType) : JKOperator {
    override val operatorText: String
        get() = TODO(token.toString())

    override val precedence: Int
        get() = when (token) {
            JavaTokenType.ASTERISK, JavaTokenType.DIV, JavaTokenType.PERC -> 3
            JavaTokenType.PLUS, JavaTokenType.MINUS -> 4
            KtTokens.ELVIS -> 7
            JavaTokenType.GT, JavaTokenType.LT, JavaTokenType.GE, JavaTokenType.LE -> 9
            JavaTokenType.EQEQ, JavaTokenType.NE, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ -> 10
            JavaTokenType.ANDAND -> 11
            JavaTokenType.OROR -> 12
            JavaTokenType.GTGTGT, JavaTokenType.GTGT, JavaTokenType.LTLT -> 7
            else -> 6 /* simple name */
        }

    companion object {
        val tokenToOperator = OPERATION_BIT_SET.types.associate {
            it to JKJavaOperatorImpl(it)
        }
    }
}

sealed class JKJavaQualifierImpl : JKQualifier {
    object DOT : JKJavaQualifierImpl()
}

class JKJavaMethodCallExpressionImpl(
    override var identifier: JKMethodSymbol,
    arguments: JKExpressionList
) : JKJavaMethodCallExpression, JKBranchElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaMethodCallExpression(this, data)

    override val arguments: JKExpressionList by child(arguments)
}

class JKJavaNewExpressionImpl(
    override val constructorSymbol: JKMethodSymbol,
    arguments: JKExpressionList
) : JKJavaNewExpression, JKBranchElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaNewExpression(this, data)

    override var arguments by child(arguments)
}

class JKJavaDefaultNewExpressionImpl(
    override val classSymbol: JKClassSymbol
) : JKJavaDefaultNewExpression, JKElementBase()

class JKJavaNewEmptyArrayImpl(override var initializer: List<JKLiteralExpression?>) : JKJavaNewEmptyArray, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaNewEmptyArray(this, data)
}

class JKJavaNewArrayImpl(override var initializer: List<JKExpression>) : JKJavaNewArray, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaNewArray(this, data)
}

sealed class JKJavaPrimitiveTypeImpl(override val jvmPrimitiveType: JvmPrimitiveType) : JKJavaPrimitiveType {
    object BOOLEAN : JKJavaPrimitiveTypeImpl(JvmPrimitiveType.BOOLEAN)
    object CHAR : JKJavaPrimitiveTypeImpl(JvmPrimitiveType.CHAR)
    object BYTE : JKJavaPrimitiveTypeImpl(JvmPrimitiveType.BYTE)
    object SHORT : JKJavaPrimitiveTypeImpl(JvmPrimitiveType.SHORT)
    object INT : JKJavaPrimitiveTypeImpl(JvmPrimitiveType.INT)
    object FLOAT : JKJavaPrimitiveTypeImpl(JvmPrimitiveType.FLOAT)
    object LONG : JKJavaPrimitiveTypeImpl(JvmPrimitiveType.LONG)
    object DOUBLE : JKJavaPrimitiveTypeImpl(JvmPrimitiveType.DOUBLE)

    companion object {
        val KEYWORD_TO_INSTANCE = listOf(
            BOOLEAN, CHAR, BYTE, SHORT, INT, FLOAT, LONG, DOUBLE
        ).associate { it.jvmPrimitiveType.javaKeywordName to it } + ("void" to JKJavaVoidType)
    }
}

object JKJavaVoidType : JKType {
    override val nullability: Nullability
        get() = Nullability.NotNull
}

class JKJavaArrayTypeImpl(override val type: JKType, override val nullability: Nullability = Nullability.Default) : JKJavaArrayType {
}

class JKReturnStatementImpl(expression: JKExpression) : JKBranchElementBase(), JKReturnStatement {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitReturnStatement(this, data)

    override val expression by child(expression)
}

class JKJavaAssertStatementImpl(condition: JKExpression, description: JKExpression) : JKJavaAssertStatement, JKBranchElementBase() {
    override val description by child(description)
    override val condition by child(condition)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaAssertStatement(this, data)
}

class JKJavaForLoopStatementImpl(initializer: JKStatement, condition: JKExpression, updater: JKStatement, body: JKStatement) :
    JKJavaForLoopStatement, JKBranchElementBase() {
    override var body by child(body)
    override var updater by child(updater)
    override var condition by child(condition)
    override var initializer by child(initializer)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaForLoopStatement(this, data)
}

class JKJavaInstanceOfExpressionImpl(expression: JKExpression, type: JKTypeElement) : JKJavaInstanceOfExpression, JKBranchElementBase() {
    override var type by child(type)
    override var expression by child(expression)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaInstanceOfExpression(this, data)
}

class JKJavaPolyadicExpressionImpl(operands: List<JKExpression>, override var tokens: List<JKOperator>) : JKJavaPolyadicExpression,
    JKBranchElementBase() {
    override var operands by children(operands)

    override fun getTokenBeforeOperand(operand: JKExpression): JKOperator? {
        val index = operands.indexOf(operand)
        return if (index < 1 || index > tokens.size) null else tokens[index - 1]
    }

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaPolyadicExpression(this, data)
}

class JKJavaAssignmentExpressionImpl(
    override var field: JKAssignableExpression,
    expression: JKExpression,
    override var operator: JKOperator
) : JKBranchElementBase(), JKJavaAssignmentExpression {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaAssignmentExpression(this, data)

    override var expression: JKExpression by child(expression)
}
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

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType

class JKJavaFieldImpl(
    modifierList: JKModifierList,
    type: JKTypeElement,
    name: JKNameIdentifier,
    initializer: JKExpression
) : JKJavaField, JKBranchElementBase() {

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaField(this, data)

    override var initializer: JKExpression by child(initializer)
    override var modifierList: JKModifierList by child(modifierList)
    override var type by child(type)
    override var name: JKNameIdentifier by child(name)
}

class JKJavaMethodImpl(
    modifierList: JKModifierList,
    returnType: JKTypeElement,
    name: JKNameIdentifier,
    valueArguments: List<JKValueArgument>,
    block: JKBlock
) : JKJavaMethod, JKBranchElementBase() {

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaMethod(this, data)

    override var modifierList: JKModifierList by child(modifierList)
    override var returnType: JKTypeElement by child(returnType)
    override var name: JKNameIdentifier by child(name)
    override var valueArguments: List<JKValueArgument> by children(valueArguments)
    override var block: JKBlock by child(block)

}

class JKJavaLiteralExpressionImpl(
    override val literal: String,
    override val type: JKLiteralExpression.LiteralType
) : JKJavaLiteralExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaLiteralExpression(this, data)
}


class JKJavaAccessModifierImpl(override val type: JKJavaAccessModifier.AccessModifierType) : JKJavaAccessModifier, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaAccessModifier(this, data)
}

class JKJavaModifierImpl(override val type: JKJavaModifier.JavaModifierType) : JKJavaModifier, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaModifier(this, data)
}

sealed class JKJavaOperatorImpl : JKOperator {
    object PLUS : JKJavaOperatorImpl()
    object MINUS : JKJavaOperatorImpl()
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

class JKJavaFieldAccessExpressionImpl(override var identifier: JKFieldSymbol) : JKJavaFieldAccessExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaFieldAccessExpression(this, data)
}

class JKJavaNewExpressionImpl(
    override val constructorSymbol: JKMethodSymbol,
    arguments: JKExpressionList
) : JKJavaNewExpression, JKBranchElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaNewExpression(this, data)

    override var arguments by child(arguments)
}

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

object JKJavaVoidType : JKType

class JKJavaArrayTypeImpl(override val type: JKType) : JKJavaArrayType

class JKReturnStatementImpl(expression: JKExpression) : JKBranchElementBase(), JKReturnStatement {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitReturnStatement(this, data)

    override val expression by child(expression)
}

class JKJavaAssignmentExpressionImpl(
    lExpression: JKExpression,
    rExpression: JKExpression/*,
    TODO operation:? */
) : JKBranchElementBase(), JKJavaAssignmentExpression {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaAssignmentExpression(this, data)

    override var lExpression: JKExpression by child(lExpression)
    override var rExpression: JKExpression by child(rExpression)
}
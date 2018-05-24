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

class JKJavaFieldImpl(
    modifierList: JKModifierList,
    type: JKType,
    name: JKNameIdentifier,
    initializer: JKExpression
) : JKJavaField, JKBranchElementBase() {
    override val valid: Boolean
        get() = true

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaField(this, data)

    override var initializer: JKExpression by child(initializer)
    override var modifierList: JKModifierList by child(modifierList)
    override var name: JKNameIdentifier by child(name)
    override var type: JKType by child(type)

}

class JKJavaLiteralExpressionImpl(
    override val literal: String, override val type: JKLiteralExpression.LiteralType
) : JKJavaLiteralExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaLiteralExpression(this, data)
}


class JKJavaAccessModifierImpl(override val type: JKJavaAccessModifier.AccessModifierType) : JKJavaAccessModifier, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaAccessModifier(this, data)
}

class JKJavaModifierImpl(override val type: JKJavaModifier.JavaModifierType) : JKJavaModifier, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaModifier(this, data)
}

class JKJavaMethodImpl(
    override var modifierList: JKModifierList,
    override var name: JKNameIdentifier,
    override var valueArguments: List<JKValueArgument>,
    override var block: JKBlock
) : JKJavaMethod, JKElementBase() {
    override val returnType: JKType
        get() = TODO()

    override val valid: Boolean
        get() = true

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaMethod(this, data)
}

sealed class JKJavaOperatorImpl : JKOperator, JKElementBase() {
    object PLUS : JKJavaOperatorImpl()
    object MINUS : JKJavaOperatorImpl()
}

sealed class JKJavaQualifierImpl : JKQualifier, JKElementBase() {
    object DOT : JKJavaQualifierImpl()
}

class JKJavaMethodCallExpressionImpl(
    override var identifier: JKMethodSymbol, override var arguments: JKExpressionList
) : JKJavaMethodCallExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaMethodCallExpression(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        arguments.accept(visitor, data)
    }
}

class JKJavaFieldAccessExpressionImpl(override var identifier: JKFieldSymbol) : JKJavaFieldAccessExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaFieldAccessExpression(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {

    }
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

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {

    }
}

class JKJavaNewArrayImpl(override var initializer: List<JKExpression>) : JKJavaNewArray, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaNewArray(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {

    }
}

sealed class JKJavaPrimitiveTypeImpl(override val name: String) : JKJavaPrimitiveType, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaPrimitiveType(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {

    }

    object BYTE : JKJavaPrimitiveTypeImpl("byte")
    object BOOLEAN : JKJavaPrimitiveTypeImpl("boolean")
    object INT : JKJavaPrimitiveTypeImpl("int")
}

class JKJavaArrayTypeImpl(type: JKType) : JKJavaArrayType, JKBranchElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaArrayType(this, data)

    override val type: JKType by child(type)
}

class JKReturnStatementImpl(expression: JKExpression) : JKBranchElementBase(), JKReturnStatement {
    // TODO accept
    override val expression by child(expression)
}

class JKJavaAssignmentExpressionImpl(
    lExpression: JKExpression,
    rExpression: JKExpression/*,
    TODO operation:? */
) : JKBranchElementBase(), JKJavaAssignmentExpression {
    override var lExpression: JKExpression by child(lExpression)
    override var rExpression: JKExpression by child(rExpression)
}
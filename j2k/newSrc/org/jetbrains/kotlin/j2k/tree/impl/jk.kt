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

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor

class JKClassImpl(modifierList: JKModifierList, name: JKNameIdentifier, override var classKind: JKClass.ClassKind) :
    JKClass,
    JKBranchElementBase() {

    override val name by child(name)
    override var modifierList by child(modifierList)
    override val valid: Boolean = true

    override var declarationList by children<JKDeclaration>()

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitClass(this, data)
}


class JKNameIdentifierImpl(override val value: String) : JKNameIdentifier, JKElementBase() {}

class JKModifierListImpl(modifiers: List<JKModifier> = emptyList()) : JKModifierList, JKBranchElementBase() {
    override var modifiers: List<JKModifier> by children(modifiers)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitModifierList(this, data)
}

class JKValueArgumentImpl(type: JKTypeElement, override val name: String) : JKValueArgument, JKBranchElementBase() {
    override var type by child(type)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitValueArgument(this, data)
}

class JKBlockImpl(statements: List<JKStatement> = emptyList()) : JKBlock, JKBranchElementBase() {
    override var statements by children(statements)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBlock(this, data)
}

class JKBinaryExpressionImpl(left: JKExpression, right: JKExpression, override var operator: JKOperator) : JKBinaryExpression,
    JKBranchElementBase() {
    override var right by child(right)
    override var left by child(left)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBinaryExpression(this, data)
}

class JKPrefixExpressionImpl(expression: JKExpression, override var operator: JKOperator) : JKPrefixExpression, JKBranchElementBase() {
    override var expression by child(expression)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPrefixExpression(this, data)
}

class JKPostfixExpressionImpl(expression: JKExpression, override var operator: JKOperator) : JKPostfixExpression, JKBranchElementBase() {
    override var expression by child(expression)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPostfixExpression(this, data)
}

class JKExpressionListImpl(expressions: List<JKExpression> = emptyList()) : JKExpressionList, JKBranchElementBase() {
    override var expressions by children(expressions)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitExpressionList(this, data)
}

class JKQualifiedExpressionImpl(
    override var receiver: JKExpression, override var operator: JKQualifier, override var selector: JKExpression
) : JKQualifiedExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitQualifiedExpression(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        receiver.accept(visitor, data)
        selector.accept(visitor, data)
    }
}

class JKExpressionStatementImpl(override val expression: JKExpression) : JKExpressionStatement, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitExpressionStatement(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
    }
}

class JKArrayAccessExpressionImpl(override var expression: JKExpression, override var indexExpression: JKExpression) :
    JKArrayAccessExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitArrayAccessExpression(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
        indexExpression.accept(visitor, data)
    }
}

class JKParenthesizedExpressionImpl(override var expression: JKExpression) : JKParenthesizedExpression, JKBranchElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitParenthesizedExpression(this, data)

}

class JKTypeCastExpressionImpl(override var expression: JKExpression, type: JKTypeElement) : JKTypeCastExpression,
    JKBranchElementBase() {
    override var type by child(type)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeCastExpression(this, data)
}

class JKTypeElementImpl(override val type: JKType) : JKTypeElement, JKElementBase()

class JKClassTypeImpl(
    override val classReference: JKClassSymbol,
    override var parameters: List<JKType>,
    override val nullability: Nullability = Nullability.Default
) : JKClassType

class JKNullLiteral : JKExpression, JKElementBase()
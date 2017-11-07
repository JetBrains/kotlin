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
import org.jetbrains.kotlin.j2k.tree.visitors.JKTransformer
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor

abstract class JKElementBase : JKElement {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitElement(this, data)

    override fun <R : JKElement, D> transform(transformer: JKTransformer<D>, data: D): R = accept(transformer, data) as R

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {}

    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {}
}


class JKClassImpl(override var modifierList: JKModifierList, override var name: JKNameIdentifier, override var classKind: JKClass.ClassKind) : JKClass, JKElementBase() {
    override var declarations: List<JKDeclaration> = mutableListOf()

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitClass(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        (listOf(modifierList, name) + declarations).forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        modifierList = modifierList.transform(transformer, data)
        name = name.transform(transformer, data)
        declarations = declarations.map { it.transform<JKDeclaration, D>(transformer, data) }
    }

}


class JKNameIdentifierImpl(override val name: String) : JKNameIdentifier, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitNameIdentifier(this, data)
}


class JKModifierListImpl : JKModifierList, JKElementBase() {
    override val modifiers = mutableListOf<JKModifier>()

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitModifierList(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        modifiers.forEach { it.accept(visitor, data) }
    }
}

class JKValueArgumentImpl(override val type: JKTypeIdentifier,
                          override val name: String) : JKValueArgument, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitValueArgument(this, data)
}

class JKBlockImpl(override var statements: List<JKStatement>) : JKBlock, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBlock(this, data)
}

class JKStringLiteralExpressionImpl(override val text: String) : JKStringLiteralExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitStringLiteralExpression(this, data)
}

class JKBinaryExpressionImpl(override val left: JKExpression, override val right: JKExpression?,
                             override val operator: JKOperatorIdentifier) : JKBinaryExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBinaryExpression(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        left.accept(visitor, data)
        operator.accept(visitor, data)
        right?.accept(visitor, data)
    }
}

class JKPrefixExpressionImpl(override val expression: JKExpression?,
                             override val operator: JKOperatorIdentifier) : JKPrefixExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPrefixExpression(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression?.accept(visitor, data)
        operator.accept(visitor, data)
    }
}

class JKPostfixExpressionImpl(override val expression: JKExpression,
                              override val operator: JKOperatorIdentifier) : JKPostfixExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPostfixExpression(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
        operator.accept(visitor, data)
    }
}

class JKExpressionListImpl(override val expressions: Array<JKExpression>) : JKExpressionList, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitExpressionList(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expressions.forEach { it.accept(visitor, data) }
    }
}

class JKQualifiedExpressionImpl(override val receiver: JKExpression, override val operator: JKQualificationIdentifier,
                                override val selector: JKStatement) : JKQualifiedExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitQualifiedExpression(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        receiver.accept(visitor, data)
        operator.accept(visitor, data)
        selector.accept(visitor, data)
    }
}

class JKArrayAccessExpressionImpl(override val expression: JKExpression, override val indexExpression: JKExpression?) : JKArrayAccessExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitArrayAccessExpression(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
        indexExpression?.accept(visitor, data)
    }
}

class JKParenthesizedExpressionImpl(override val expression: JKExpression?) : JKParenthesizedExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitParenthesizedExpression(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression?.accept(visitor, data)
    }
}

class JKTypeCastExpressionImpl(override val expression: JKExpression?, override val type: JKTypeReference?) : JKTypeCastExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeCastExpression(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression?.accept(visitor, data)
        type?.accept(visitor, data)
    }
}

class JKTypeReferenceImpl() : JKTypeReference, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeReference(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {

    }
}
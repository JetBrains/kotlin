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


class JKNameIdentifierImpl(override var name: String) : JKNameIdentifier, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitNameIdentifier(this, data)
}

class JKModifierListImpl : JKModifierList, JKElementBase() {
    override var modifiers = listOf<JKModifier>()

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitModifierList(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        modifiers.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        modifiers = modifiers.map { it.transform<JKModifier, D>(transformer, data) }
    }
}

class JKValueArgumentImpl(override var type: JKTypeIdentifier,
                          override val name: String) : JKValueArgument, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitValueArgument(this, data)
    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        type = type.transform(transformer, data)
    }
}

class JKBlockImpl(override var statements: List<JKStatement>) : JKBlock, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBlock(this, data)
    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        statements = statements.map { it.transform<JKStatement, D>(transformer, data) }
    }
}

class JKStringLiteralExpressionImpl(override val text: String) : JKStringLiteralExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitStringLiteralExpression(this, data)
}

class JKBinaryExpressionImpl(override var left: JKExpression, override var right: JKExpression?,
                             override var operator: JKOperatorIdentifier) : JKBinaryExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBinaryExpression(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        left.accept(visitor, data)
        operator.accept(visitor, data)
        right?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        left = left.transform(transformer, data)
        right = right?.transform(transformer, data)
        operator = operator.transform(transformer, data)
    }
}

class JKPrefixExpressionImpl(override var expression: JKExpression,
                             override var operator: JKOperatorIdentifier) : JKPrefixExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPrefixExpression(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression?.accept(visitor, data)
        operator.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        expression = expression?.transform(transformer, data)
        operator = operator.transform(transformer, data)
    }
}

class JKPostfixExpressionImpl(override var expression: JKExpression,
                              override var operator: JKOperatorIdentifier) : JKPostfixExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPostfixExpression(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression?.accept(visitor, data)
        operator.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        expression = expression?.transform(transformer, data)
        operator = operator.transform(transformer, data)
    }
}

class JKExpressionListImpl(override var expressions: Array<JKExpression>) : JKExpressionList, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitExpressionList(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expressions.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        expressions = expressions.map { it.transform<JKExpression, D>(transformer, data) }.toTypedArray()
    }
}

class JKQualifiedExpressionImpl(override var receiver: JKExpression, override var operator: JKQualificationIdentifier,
                                override var selector: JKStatement) : JKQualifiedExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitQualifiedExpression(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        receiver.accept(visitor, data)
        operator.accept(visitor, data)
        selector.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        receiver = receiver.transform(transformer, data)
        operator = operator.transform(transformer, data)
        selector = selector.transform(transformer, data)
    }
}

class JKArrayAccessExpressionImpl(override var expression: JKExpression, override var indexExpression: JKExpression?) : JKArrayAccessExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitArrayAccessExpression(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
        indexExpression?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        expression = expression.transform(transformer, data)
        indexExpression = expression.transform(transformer, data)
    }
}

class JKParenthesizedExpressionImpl(override var expression: JKExpression?) : JKParenthesizedExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitParenthesizedExpression(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        expression = expression?.transform(transformer, data)
    }
}

class JKTypeCastExpressionImpl(override var expression: JKExpression?, override var type: JKTypeReference?) : JKTypeCastExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeCastExpression(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression?.accept(visitor, data)
        type?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        expression = expression?.transform(transformer, data)
        type = type?.transform(transformer, data)
    }
}

class JKTypeReferenceImpl(override val parameters: List<JKTypeReference>) : JKTypeReference, JKElementBase() {


    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeReference(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {

    }
}
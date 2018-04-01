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
import org.jetbrains.kotlin.j2k.tree.JKReference.JKReferenceType
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor
import kotlin.reflect.KProperty

class JKChild<T : JKElement>(var value: T) {
    operator fun getValue(thisRef: JKElement, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: JKElement, property: KProperty<*>, v: T) {
        value.parent = null
        v.parent = thisRef
        value = v
    }
}

class JKListChild<T : JKElement>(var value: List<T>) {
    operator fun getValue(thisRef: JKElement, property: KProperty<*>): List<T> {
        return value
    }

    operator fun setValue(thisRef: JKElement, property: KProperty<*>, v: List<T>) {
        value.forEach { it.parent = null }
        v.forEach { it.parent = thisRef }
        value = v
    }
}

abstract class JKElementBase : JKElement {
    override var parent: JKElement? = null

    fun <D : JKElement> D.setParent(p: JKElement): D {
        parent = p
        return this
    }

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitElement(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {}
}


class JKClassImpl(modifierList: JKModifierList, override val name: JKNameIdentifier, override var classKind: JKClass.ClassKind) : JKClass,
    JKElementBase() {
    override var modifierList by JKChild(modifierList)
    override val valid: Boolean = true

    override var declarations by JKListChild(emptyList<JKDeclaration>())

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitClass(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        modifierList.accept(visitor, data)
        name.accept(visitor, data)
        declarations.forEach { it.accept(visitor, data) }
    }
}


class JKNameIdentifierImpl(override val name: String) : JKNameIdentifier, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitNameIdentifier(this, data)
}

class JKModifierListImpl(modifiers: List<JKModifier> = emptyList()) : JKModifierList, JKElementBase() {
    override var modifiers by JKListChild(modifiers)

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitModifierList(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        modifiers.forEach { it.accept(visitor, data) }
    }
}

class JKValueArgumentImpl(type: JKType, override val name: String) : JKValueArgument, JKElementBase() {
    override var type by JKChild(type)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitValueArgument(this, data)
}

class JKBlockImpl(statements: List<JKStatement> = emptyList()) : JKBlock, JKElementBase() {
    override var statements by JKListChild(statements)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBlock(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        statements.forEach { it.accept(visitor, data) }
    }
}

class JKBinaryExpressionImpl(left: JKExpression, right: JKExpression, override var operator: JKOperator) : JKBinaryExpression,
    JKElementBase() {
    override var right by JKChild(right)
    override var left by JKChild(left)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBinaryExpression(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        left.accept(visitor, data)
        right.accept(visitor, data)
    }
}

class JKPrefixExpressionImpl(expression: JKExpression, override var operator: JKOperator) : JKPrefixExpression, JKElementBase() {
    override var expression by JKChild(expression)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPrefixExpression(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
    }
}

class JKPostfixExpressionImpl(expression: JKExpression, override var operator: JKOperator) : JKPostfixExpression, JKElementBase() {
    override var expression by JKChild(expression)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPostfixExpression(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
    }
}

class JKExpressionListImpl(expressions: List<JKExpression> = emptyList()) : JKExpressionList, JKElementBase() {
    override var expressions by JKListChild(expressions)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitExpressionList(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expressions.forEach { it.accept(visitor, data) }
    }
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

class JKParenthesizedExpressionImpl(override var expression: JKExpression) : JKParenthesizedExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitParenthesizedExpression(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
    }
}

class JKTypeCastExpressionImpl(override var expression: JKExpression, override var type: JKType) : JKTypeCastExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeCastExpression(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
        type.accept(visitor, data)
    }
}

class JKClassTypeImpl(
    override val classReference: JKClassReference,
    override val parameters: List<JKType>,
    override val nullability: Nullability = Nullability.Default
) : JKClassType, JKElementBase() {

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitClassType(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {

    }
}

class JKFieldReferenceImpl(override val target: JKField, override val referenceType: JKReferenceType) : JKFieldReference, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitFieldReference(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {

    }
}

class JKClassReferenceImpl(override val target: JKClass, override val referenceType: JKReferenceType) : JKClassReference, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitClassReference(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {

    }
}

class JKMethodReferenceImpl(override val target: JKMethod, override val referenceType: JKReferenceType) : JKMethodReference,
    JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitMethodReference(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {

    }
}
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
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private class JKChild<T : JKElement>(val value: Int) : ReadWriteProperty<JKMutableBranchElement, T> {
    override operator fun getValue(thisRef: JKMutableBranchElement, property: KProperty<*>): T {
        return thisRef.children[value] as T
    }

    override operator fun setValue(thisRef: JKMutableBranchElement, property: KProperty<*>, value: T) {
        thisRef.children[this.value].parent = null
        value.parent = thisRef
        thisRef.children[this.value] = value
    }
}

class JKListChild<T : JKTreeElement>() : ReadWriteProperty<JKMutableBranchElement, List<T>> {
    override operator fun getValue(thisRef: JKMutableBranchElement, property: KProperty<*>): List<T> {
        return thisRef.children.toList() as List<T>
    }

    override operator fun setValue(thisRef: JKMutableBranchElement, property: KProperty<*>, value: List<T>) {
        thisRef.children.forEach { it.parent = null }
        value.forEach { it.parent = thisRef }
        thisRef.children.clear()
        thisRef.children.addAll(value as List<JKElement>)
    }
}

abstract class JKElementBase : JKElement {
    override var parent: JKTreeElement? = null

    fun <D : JKElement> D.setParent(p: JKElement): D {
        parent = p
        return this
    }

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitElement(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {}
}

interface JKMutableBranchElement : JKBranchElement {
    override val children: MutableList<JKElement>
}

abstract class JKBranchElementBase : JKElementBase(), JKMutableBranchElement {
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        children.forEach { it.accept(visitor, data) }
    }

    protected var childNum = 0
    protected fun <T : JKElement, U : T> child(v: U): ReadWriteProperty<JKMutableBranchElement, T> {
        return JKChild(childNum++)
    }

    override val children: MutableList<JKElement> = mutableListOf()
}

abstract class JKElementListBase: JKElementBase(), JKMutableBranchElement {
    override val children: MutableList<JKElement> = mutableListOf()

    protected inline fun <reified T : JKElement> children(): JKListChild<T> {
        return JKListChild()
    }

    protected inline fun <reified T : JKElement> children(v: List<T>): JKListChild<T> {
        children.addAll(v)
        return JKListChild()
    }
}

class JKDeclarationListImpl : JKElementListBase(), JKDeclarationList {
    override var declarations: List<JKUniverseDeclaration> by children()
}


class JKClassImpl(modifierList: JKModifierList, name: JKNameIdentifier, override var classKind: JKClass.ClassKind) :
    JKUniverseClass,
    JKBranchElementBase() {

    override val name by child(name)
    override var modifierList by child(modifierList)
    override val valid: Boolean = true

    override val declarationList by child(JKDeclarationListImpl())

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitUniverseClass(this, data)
}


class JKNameIdentifierImpl(override val name: String) : JKNameIdentifier, JKElementBase() {}

class JKModifierListImpl(modifiers: List<JKModifier> = emptyList()) : JKModifierList, JKElementListBase() {
    override var modifiers: List<JKModifier> by children(modifiers)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitModifierList(this, data)
}

class JKValueArgumentImpl(type: JKType, override val name: String) : JKValueArgument, JKBranchElementBase() {
    override var type by child(type)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitValueArgument(this, data)
}

class JKBlockImpl(statements: List<JKStatement> = emptyList()) : JKBlock, JKElementListBase() {
    override var statements by children(statements)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBlock(this, data)
}

class JKBinaryExpressionImpl(left: JKExpression, right: JKExpression, override var operator: JKOperator) : JKBinaryExpression,
    JKBranchElementBase() {
    override var right by child(right)
    override var left by child(left)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitBinaryExpression(this, data)
}

class JKPrefixExpressionImpl(expression: JKExpression, operator: JKOperator) : JKPrefixExpression, JKBranchElementBase() {
    override var operator by child(operator)
    override var expression by child(expression)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPrefixExpression(this, data)
}

class JKPostfixExpressionImpl(expression: JKExpression, override var operator: JKOperator) : JKPostfixExpression, JKBranchElementBase() {
    override var expression by child(expression)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitPostfixExpression(this, data)
}

class JKExpressionListImpl(expressions: MutableList<JKExpression> = mutableListOf()) : JKExpressionList, JKElementListBase() {
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

class JKTypeCastExpressionImpl(override var expression: JKExpression, override var type: JKType) : JKTypeCastExpression, JKBranchElementBase() {
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

class JKClassReferenceImpl(override val target: JKClass, override val referenceType: JKReferenceType) : JKClassReference,
    JKElementBase() {
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
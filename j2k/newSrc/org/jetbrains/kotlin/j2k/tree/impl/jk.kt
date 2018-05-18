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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class JKChild<T : JKElement>(val value: Int) : ReadWriteProperty<JKBranchElementBase, T> {
    override operator fun getValue(thisRef: JKBranchElementBase, property: KProperty<*>): T {
        return thisRef.children[value] as T
    }

    override operator fun setValue(thisRef: JKBranchElementBase, property: KProperty<*>, value: T) {
        (thisRef.children[this.value] as T).detach()
        value.attach(thisRef)
        thisRef.children[this.value] = value
    }
}

class JKListChild<T : JKElement>(val value: Int) : ReadWriteProperty<JKBranchElementBase, List<T>> {
    override operator fun getValue(thisRef: JKBranchElementBase, property: KProperty<*>): List<T> {
        return thisRef.children.toList() as List<T>
    }

    override operator fun setValue(thisRef: JKBranchElementBase, property: KProperty<*>, value: List<T>) {
        (thisRef.children[this.value] as List<T>).forEach { it.detach() }
        value.forEach { it.attach(thisRef) }
        thisRef.children[this.value] = value
    }
}

abstract class JKElementBase : JKTreeElement {
    override var parent: JKElement? = null

    final override fun detach() {
        val prevParent = parent
        assert(prevParent != null)
        parent = null
        onDetach(prevParent!!)
    }

    open fun onDetach(from: JKElement) {

    }

    final override fun attach(to: JKElement) {
        assert(parent == null)
        parent = to
        onAttach()
    }

    open fun onAttach() {

    }

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitElement(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {}
}

abstract class JKBranchElementBase : JKElementBase(), JKBranchElement {
    protected var childNum = 0
    protected fun <T : JKTreeElement, U : T> child(v: U): ReadWriteProperty<JKBranchElementBase, T> {
        children.add(childNum, v)
        return JKChild(childNum++)
    }

    protected inline fun <reified T : JKTreeElement> child(): ReadWriteProperty<JKBranchElementBase, List<T>> {
        children[childNum] = listOf<T>()
        return JKListChild(childNum++)
    }

    protected inline fun <reified T : JKTreeElement> child(v: List<T>): ReadWriteProperty<JKBranchElementBase, List<T>> {
        children[childNum] = v
        return JKListChild(childNum++)
    }

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        children.forEach {
            if (it is JKTreeElement)
                it.accept(visitor, data)
            else
                (it as? List<JKTreeElement>)?.forEach { it.accept(visitor, data) }
        }
    }

    override val children: MutableList<Any> = mutableListOf()
}

class JKClassImpl(modifierList: JKModifierList, name: JKNameIdentifier, override var classKind: JKClass.ClassKind) :
    JKUniverseClass,
    JKBranchElementBase() {

    override val name by child(name)
    override var modifierList by child(modifierList)
    override val valid: Boolean = true

    override var declarationList by child<JKUniverseDeclaration>()

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitUniverseClass(this, data)
}


class JKNameIdentifierImpl(override val value: String) : JKNameIdentifier, JKElementBase() {}

class JKModifierListImpl(modifiers: List<JKModifier> = emptyList()) : JKModifierList, JKBranchElementBase() {
    override var modifiers: List<JKModifier> by child(modifiers)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitModifierList(this, data)
}

class JKValueArgumentImpl(type: JKType, override val name: String) : JKValueArgument, JKBranchElementBase() {
    override var type by child(type)
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitValueArgument(this, data)
}

class JKBlockImpl(statements: List<JKStatement> = emptyList()) : JKBlock, JKBranchElementBase() {
    override var statements by child(statements)
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

class JKExpressionListImpl(expressions: List<JKExpression> = emptyList()) : JKExpressionList, JKBranchElementBase() {
    override var expressions by child(expressions)
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

class JKTypeCastExpressionImpl(override var expression: JKExpression, override var type: JKType) : JKTypeCastExpression,
    JKBranchElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTypeCastExpression(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
        type.accept(visitor, data)
    }
}

class JKClassTypeImpl(
    override val classReference: JKSymbol<JKClass>?,
    override val parameters: List<JKType>,
    override val nullability: Nullability = Nullability.Default
) : JKClassType, JKElementBase() {

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitClassType(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {

    }
}


abstract class JKReferenceBase<T : JKReferenceTarget>(val resolve: () -> PsiElement?) : JKElementBase(), JKReference {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitReference(this, data)

    override var target: T? = null
}

class JKFieldReferenceImpl(resolve: () -> PsiElement?) : JKFieldReference, JKReferenceBase<JKField>(resolve) {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitFieldReference(this, data)
}

class JKClassReferenceImpl(resolve: () -> PsiElement?) : JKClassReference, JKReferenceBase<JKClass>(resolve) {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitClassReference(this, data)
}

class JKMethodReferenceImpl(resolve: () -> PsiElement?) : JKMethodReference, JKReferenceBase<JKMethod>(resolve) {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitMethodReference(this, data)
}
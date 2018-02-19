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

package org.jetbrains.kotlin.j2k.tree

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.tree.visitors.JKTransformer
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor


interface JKElement {
    fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R
    fun <R : JKElement, D> transform(transformer: JKTransformer<D>, data: D): R

    fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D)
    fun <D> transformChildren(transformer: JKTransformer<D>, data: D)
}

interface JKClass : JKDeclaration, JKModifierListOwner {
    val name: JKNameIdentifier
    val declarations: List<JKDeclaration>
    val classKind: ClassKind

    enum class ClassKind {
        ABSTRACT, ANNOTATION, CLASS, ENUM, INTERFACE
    }
}

interface JKStatement : JKElement

interface JKExpression : JKStatement

interface JKBinaryExpression : JKExpression {
    val left: JKExpression
    val right: JKExpression?
    val operator: JKOperatorIdentifier
}

interface JKUnaryExpression : JKExpression {
    val expression: JKExpression?
    val operator: JKOperatorIdentifier
}

interface JKPrefixExpression : JKUnaryExpression

interface JKPostfixExpression : JKUnaryExpression {
    override val expression: JKExpression
}

interface JKQualifiedExpression : JKExpression {
    val receiver: JKExpression
    val operator: JKQualificationIdentifier
    val selector: JKStatement
}

interface JKMethodCallExpression : JKExpression {
    val identifier: JKMethodReference
    val arguments: JKExpressionList
}

interface JKFieldAccessExpression : JKExpression {
    val identifier: JKFieldReference
}

interface JKArrayAccessExpression : JKExpression {
    val expression: JKExpression
    val indexExpression: JKExpression?
}

interface JKParenthesizedExpression : JKExpression {
    val expression: JKExpression?
}

interface JKTypeCastExpression : JKExpression {
    val expression: JKExpression?
    val type: JKTypeReference?
}

interface JKExpressionList : JKElement {

}

interface JKReference : JKElement {

}

interface JKExternalReference {
    fun resolve(): PsiElement
}

interface JKMethodReference : JKReference {

}

interface JKFieldReference : JKReference {

}

interface JKClassReference : JKReference {

}

interface JKTypeReference : JKReference {
    val parameters: List<JKTypeReference>
}

interface JKOperatorIdentifier : JKIdentifier

interface JKQualificationIdentifier : JKIdentifier

interface JKLoop : JKStatement

interface JKDeclaration : JKElement

interface JKBlock : JKElement {
    val statements: List<JKStatement>
}

interface JKIdentifier : JKElement

interface JKTypeIdentifier : JKIdentifier

interface JKNameIdentifier : JKIdentifier {
    val name: String
}

interface JKLiteralExpression : JKExpression

interface JKModifierList : JKElement {
    val modifiers: List<JKModifier>
}

interface JKModifier : JKElement

interface JKAccessModifier : JKModifier

interface JKValueArgument : JKElement {
    val type: JKTypeIdentifier
    val name: String
}

interface JKStringLiteralExpression : JKLiteralExpression {
    val text: String
}

interface JKModalityModifier : JKModifier
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

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor

interface JKTreeElement : JKElement {
    fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R

    fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D)
}

interface JKUniverseDeclaration : JKTreeElement, JKDeclaration

interface JKDeclarationList : JKElement {
    val declarations: List<JKDeclaration>
}

interface JKUDeclarationList : JKDeclarationList {
    override var declarations: List<JKUniverseDeclaration>
}

interface JKUniverseClass : JKUniverseDeclaration, JKClass {
    override val declarationList: JKUDeclarationList
}

interface JKModifier : JKTreeElement

interface JKModifierList : JKBranchElement, JKTreeElement {
    var modifiers: List<JKModifier>
}

interface JKAccessModifier : JKModifier

interface JKModalityModifier : JKModifier

interface JKReference : JKTreeElement {
    val target: JKReferenceTarget
    val referenceType: JKReferenceType

    enum class JKReferenceType {
        U2U, U2M, M2U
    }
}

interface JKMethodReference : JKReference {
    override val target: JKMethod
}

interface JKFieldReference : JKReference {
    override val target: JKField
}

interface JKClassReference : JKReference {
    override val target: JKClass
}

interface JKType : JKTreeElement

interface JKClassType : JKType {
    val classReference: JKClassReference
    val nullability: Nullability
    val parameters: List<JKType>
}

interface JKStatement : JKTreeElement

interface JKLoop : JKStatement

interface JKBlock : JKTreeElement {
    var statements: List<JKStatement>
}

interface JKIdentifier : JKTreeElement

interface JKNameIdentifier : JKIdentifier {
    val name: String
}

interface JKExpression : JKTreeElement

interface JKExpressionStatement : JKStatement {
    val expression: JKExpression
}

interface JKBinaryExpression : JKExpression {
    var left: JKExpression
    var right: JKExpression
    var operator: JKOperator
}

interface JKUnaryExpression : JKExpression {
    var expression: JKExpression
    var operator: JKOperator
}

interface JKPrefixExpression : JKUnaryExpression

interface JKPostfixExpression : JKUnaryExpression

interface JKQualifiedExpression : JKExpression {
    val receiver: JKExpression
    val operator: JKQualifier
    val selector: JKExpression
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
    val indexExpression: JKExpression
}

interface JKParenthesizedExpression : JKExpression {
    val expression: JKExpression
}

interface JKTypeCastExpression : JKExpression {
    val expression: JKExpression
    val type: JKType
}

interface JKExpressionList : JKTreeElement {
    var expressions: List<JKExpression>
}

interface JKLiteralExpression : JKExpression {
    val literal: String
    val type: LiteralType

    enum class LiteralType {
        STRING, BOOLEAN, NULL
    }
}

interface JKValueArgument : JKTreeElement {
    var type: JKType
    val name: String
}

interface JKStringLiteralExpression : JKLiteralExpression {
    val text: String
}
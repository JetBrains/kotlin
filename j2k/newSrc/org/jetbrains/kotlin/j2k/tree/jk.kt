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

import org.jetbrains.kotlin.j2k.tree.impl.JKClassSymbol
import org.jetbrains.kotlin.j2k.tree.impl.JKFieldSymbol
import org.jetbrains.kotlin.j2k.tree.impl.JKMethodSymbol
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor

interface JKTreeElement : JKElement {
    fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R

    fun <R> accept(visitor: JKVisitor<R, Nothing?>): R = accept(visitor, null)

    fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D)

    fun acceptChildren(visitor: JKVisitor<Unit, Nothing?>) = acceptChildren(visitor, null)
}

interface JKDeclaration : JKTreeElement

interface JKClass : JKDeclaration, JKModifierListOwner {
    val name: JKNameIdentifier
    var declarationList: List<JKDeclaration>
    var classKind: ClassKind

    enum class ClassKind {
        ABSTRACT, ANNOTATION, CLASS, ENUM, INTERFACE
    }
}

interface JKMethod : JKDeclaration, JKModifierListOwner {
    val name: JKNameIdentifier
    var valueArguments: List<JKValueArgument>
    val returnType: JKTypeElement
}

interface JKField : JKDeclaration, JKModifierListOwner {
    val type: JKTypeElement
    val name: JKNameIdentifier
    val initializer: JKExpression
}

interface JKLocalVariable : JKField

interface JKModifier : JKTreeElement

interface JKModifierList : JKTreeElement {
    var modifiers: List<JKModifier>
}

interface JKAccessModifier : JKModifier

interface JKModalityModifier : JKModifier

interface JKTypeElement : JKTreeElement {
    val type: JKType
}

interface JKStatement : JKTreeElement

interface JKBlock : JKTreeElement {
    var statements: List<JKStatement>
}

interface JKIdentifier : JKTreeElement

interface JKNameIdentifier : JKIdentifier {
    val value: String
}

interface JKExpression : JKTreeElement

interface JKExpressionStatement : JKStatement {
    val expression: JKExpression
}

interface JKDeclarationStatement : JKStatement {
    val declaredStatements: List<JKDeclaration>
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
    val identifier: JKMethodSymbol
    val arguments: JKExpressionList
}

interface JKFieldAccessExpression : JKExpression {
    val identifier: JKFieldSymbol
}

interface JKClassAccessExpression : JKExpression {
    val identifier: JKClassSymbol
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
    val type: JKTypeElement
}

interface JKExpressionList : JKTreeElement {
    var expressions: List<JKExpression>
}

interface JKLiteralExpression : JKExpression {
    val literal: String
    val type: LiteralType

    enum class LiteralType {
        STRING, CHAR, BOOLEAN, NULL, INT, LONG, FLOAT, DOUBLE
    }
}

interface JKValueArgument : JKTreeElement {
    var type: JKTypeElement
    val name: String
}

interface JKStringLiteralExpression : JKLiteralExpression {
    val text: String
}

interface JKStubExpression : JKExpression

interface JKLoopStatement : JKStatement {
    var body: JKStatement
}

interface JKBlockStatement : JKStatement {
    var block: JKBlock
}

interface JKThisExpression : JKExpression

interface JKSuperExpression : JKExpression

interface JKWhileStatement : JKLoopStatement {
    var condition: JKExpression
}

interface JKDoWhileStatement : JKLoopStatement {
    var condition: JKExpression
}

interface JKSwitchStatement : JKStatement {
    var expression: JKExpression
    var block: JKBlock
}

interface JKSwitchLabelStatement : JKStatement {
    var expression: JKExpression
}

interface JKSwitchDefaultLabelStatement : JKStatement

interface JKBreakStatement : JKStatement

interface JKIfStatement : JKStatement {
    var condition: JKExpression
    var thenBranch: JKStatement
}

interface JKIfElseStatement : JKIfStatement {
    var elseBranch: JKStatement
}

interface JKIfElseExpression : JKExpression {
    var condition: JKExpression
    var thenBranch: JKExpression
    var elseBranch: JKExpression
}
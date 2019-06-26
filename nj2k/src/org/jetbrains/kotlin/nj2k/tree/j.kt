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

package org.jetbrains.kotlin.nj2k.tree

import org.jetbrains.kotlin.nj2k.tree.impl.*

abstract class JKField : JKVariable(), JKVisibilityOwner, JKMutabilityOwner, JKModalityOwner, JKOtherModifiersOwner, JKAnnotationListOwner

abstract class JKJavaField : JKField()

abstract class JKJavaMethod : JKMethod(), JKBranchElement {
    abstract var throwsList: List<JKTypeElement>
}

interface JKJavaMethodCallExpression : JKMethodCallExpression

abstract class JKClassBody : JKTreeElement, JKBranchElementBase() {
    abstract var declarations: List<JKDeclaration>

    val leftBrace = JKTokenElementImpl("{")
    val rightBrace = JKTokenElementImpl("}")
}

abstract class JKEmptyClassBody : JKClassBody() {
    override var declarations: List<JKDeclaration> = emptyList()
}

interface JKJavaNewExpression : JKExpression, JKTypeArgumentListOwner, PsiOwner {
    val classSymbol: JKClassSymbol
    var arguments: JKArgumentList
    var classBody: JKClassBody
}

fun JKJavaNewExpression.isAnonymousClass() =
    classBody !is JKEmptyClassBody


interface JKJavaDefaultNewExpression : JKExpression {
    val classSymbol: JKClassSymbol
}


interface JKJavaNewEmptyArray : JKExpression {
    val type: JKTypeElement
    var initializer: List<JKExpression>
}

interface JKJavaNewArray : JKExpression {
    val type: JKTypeElement
    var initializer: List<JKExpression>
}

interface JKJavaLiteralExpression : JKLiteralExpression

abstract class JKReturnStatement : JKStatement() {
    abstract val expression: JKExpression
    abstract var label: JKLabel
}

abstract class JKJavaAssertStatement : JKStatement() {
    abstract val condition: JKExpression
    abstract val description: JKExpression
}

abstract class JKJavaForLoopStatement : JKLoopStatement() {
    abstract var initializer: JKStatement
    abstract var condition: JKExpression
    abstract var updaters: List<JKStatement>
}


interface JKJavaPolyadicExpression : JKExpression {
    var operands: List<JKExpression>
    var tokens: List<JKOperator>

    fun getTokenBeforeOperand(operand: JKExpression): JKOperator?
}

interface JKJavaAssignmentExpression : JKExpression, JKBranchElement {
    var field: JKAssignableExpression
    var expression: JKExpression
    var operator: JKOperator
}

abstract class JKJavaThrowStatement : JKStatement() {
    abstract var exception: JKExpression
}

abstract class JKJavaTryStatement : JKStatement() {
    abstract var resourceDeclarations: List<JKDeclaration>
    abstract var tryBlock: JKBlock
    abstract var finallyBlock: JKBlock
    abstract var catchSections: List<JKJavaTryCatchSection>
}

interface JKJavaTryCatchSection : JKTreeElement {
    var parameter: JKParameter
    var block: JKBlock
}


abstract class JKJavaSwitchStatement : JKStatement() {
    abstract var expression: JKExpression
    abstract var cases: List<JKJavaSwitchCase>
}

interface JKJavaSwitchCase : JKTreeElement {
    fun isDefault(): Boolean
    var statements: List<JKStatement>
}

interface JKJavaDefaultSwitchCase : JKJavaSwitchCase {
    override fun isDefault(): Boolean = true
}

interface JKJavaLabelSwitchCase : JKJavaSwitchCase {
    override fun isDefault(): Boolean = false
    var label: JKExpression
}

abstract class JKJavaContinueStatement : JKStatement()

abstract class JKJavaSynchronizedStatement : JKStatement(), JKBranchElement {
    abstract val lockExpression: JKExpression
    abstract val body: JKBlock
}

abstract class JKJavaAnnotationMethod : JKMethod(), JKBranchElement {
    abstract val defaultValue: JKAnnotationMemberValue

    override var otherModifierElements by children<JKOtherModifierElement>()
    override var visibilityElement by child(JKVisibilityModifierElementImpl(Visibility.PUBLIC))
    override var modalityElement by child(JKModalityModifierElementImpl(Modality.FINAL))
}

abstract class JKJavaStaticInitDeclaration : JKDeclaration(), JKBranchElement {
    abstract var block: JKBlock
}
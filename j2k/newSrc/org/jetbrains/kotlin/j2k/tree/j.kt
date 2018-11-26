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

import org.jetbrains.kotlin.idea.search.usagesSearch.constructor
import org.jetbrains.kotlin.j2k.tree.impl.*

interface JKField : JKVariable, JKVisibilityOwner, JKMutabilityOwner, JKModalityOwner, JKExtraModifiersOwner

interface JKJavaField : JKField, JKBranchElement

interface JKJavaMethod : JKMethod, JKBranchElement {
}

interface JKJavaMethodCallExpression : JKMethodCallExpression

interface JKClassBody : JKTreeElement, JKBranchElement {
    var declarations: List<JKDeclaration>
}

interface JKEmptyClassBody : JKClassBody

interface JKJavaNewExpression : JKExpression, JKTypeArgumentListOwner {
    val classSymbol: JKClassSymbol
    var arguments: JKExpressionList
    var classBody: JKClassBody
}

fun JKJavaNewExpression.isAnonymousClass() =
    classBody !is JKEmptyClassBody

fun JKJavaNewExpression.constructorIsPresent(): Boolean {
    if (arguments.expressions.isNotEmpty()) return true
    val symbol = classSymbol
    return when (symbol) {
        is JKMultiverseClassSymbol -> symbol.target.constructors.isNotEmpty()
        is JKMultiverseKtClassSymbol -> symbol.target.constructor != null
        is JKUniverseClassSymbol -> symbol.target.classBody.declarations.any { it is JKKtConstructor }
        is JKUnresolvedClassSymbol -> true //TODO ???
        else -> TODO(symbol::class.toString())
    }
}

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

interface JKReturnStatement : JKStatement {
    val expression: JKExpression
}

interface JKJavaAssertStatement : JKStatement {
    val condition: JKExpression
    val description: JKExpression
}

interface JKJavaForLoopStatement : JKLoopStatement {
    var initializer: JKStatement
    var condition: JKExpression
    var updater: JKStatement
}

interface JKJavaInstanceOfExpression : JKExpression {
    var expression: JKExpression
    var type: JKTypeElement
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

interface JKJavaThrowStatement : JKStatement {
    var exception: JKExpression
}

interface JKJavaTryStatement : JKStatement {
    var resourceDeclarations: List<JKDeclaration>
    var tryBlock: JKBlock
    var finallyBlock: JKBlock
    var catchSections: List<JKJavaTryCatchSection>
}

interface JKJavaTryCatchSection : JKTreeElement {
    var parameter: JKParameter
    var block: JKBlock
}


interface JKJavaSwitchStatement : JKStatement {
    var expression: JKExpression
    var cases: List<JKJavaSwitchCase>
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

interface JKJavaContinueStatement: JKStatement
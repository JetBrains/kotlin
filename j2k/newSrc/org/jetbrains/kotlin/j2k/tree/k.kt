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

import org.jetbrains.kotlin.j2k.tree.impl.JKMethodSymbol

interface JKKtProperty : JKField {
    val getter: JKBlock
    val setter: JKBlock
}

interface JKKtFunction : JKMethod {
}

interface JKKtConstructor : JKDeclaration, JKModifierListOwner, JKMethod, JKBranchElement {
    override var name: JKNameIdentifier
    override var parameters: List<JKParameter>
    var delegationCall: JKExpression
}

interface JKKtPrimaryConstructor : JKKtConstructor

interface JKKtAssignmentStatement : JKStatement {
    var field: JKAssignableExpression
    var expression: JKExpression
    var operator: JKOperator
}

interface JKKtCall : JKMethodCallExpression

interface JKKtModifier : JKModifier {
    val type: KtModifierType

    enum class KtModifierType {
        ACTUAL, ABSTRACT, ANNOTATION, COMPANION, CONST, CROSSINLINE, DATA, ENUM, EXPECT, EXTERNAL, FINAL, INFIX, INLINE, INNER,
        LATEINIT, NOINLINE, OPEN, OPERATOR, OUT, OVERRIDE, REIFIED, SEALED, SUSPEND, TAILREC, VARARG, PRIVATE, PROTECTED
    }
}

interface JKKtMethodCallExpression : JKMethodCallExpression

interface JKKtAlsoCallExpression : JKKtMethodCallExpression {
    var statement: JKStatement
    val parameterName: String
}

interface JKKtLiteralExpression : JKLiteralExpression

interface JKKtWhenStatement : JKStatement {
    var expression: JKExpression
    var cases: List<JKKtWhenCase>
}

interface JKKtWhenCase : JKTreeElement {
    var labels: List<JKKtWhenLabel>
    var statement: JKStatement
}

interface JKKtWhenLabel : JKTreeElement

interface JKKtElseWhenLabel : JKKtWhenLabel

interface JKKtValueWhenLabel : JKKtWhenLabel {
    var expression: JKExpression
}

interface JKKtIsExpression : JKExpression {
    var expression: JKExpression
    var type: JKTypeElement
}

interface JKKtInitDeclaration : JKDeclaration {
    var block: JKBlock
}

interface JKKtOperatorExpression : JKExpression {
    var receiver: JKExpression
    var identifier: JKMethodSymbol
    var argument: JKExpression
}

interface JKKtConvertedFromForLoopSyntheticWhileStatement : JKStatement {
    var variableDeclaration: JKStatement
    var whileStatement: JKWhileStatement
}


interface JKKtThrowExpression : JKExpression {
    var exception: JKExpression
}

interface JKKtTryExpression : JKExpression {
    var tryBlock: JKBlock
    var finallyBlock: JKBlock
    var catchSections: List<JKKtTryCatchSection>
}

interface JKKtTryCatchSection : JKTreeElement {
    var parameter: JKParameter
    var block: JKBlock
}
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

interface JKKtGetterOrSetter : JKTreeElement, JKVisibilityOwner, JKBranchElement {
    var body: JKStatement
    val kind: Kind

    enum class Kind {
        GETTER, SETTER
    }
}

interface JKKtEmptyGetterOrSetter : JKKtGetterOrSetter

abstract class JKKtProperty : JKField(), PsiOwner {
    abstract var getter: JKKtGetterOrSetter
    abstract var setter: JKKtGetterOrSetter
}


abstract class JKKtFunction : JKMethod(), PsiOwner

abstract class JKKtConstructor : JKMethod(), JKOtherModifiersOwner {
    abstract var delegationCall: JKExpression
}

abstract class JKKtPrimaryConstructor : JKKtConstructor()

abstract class JKKtAssignmentStatement : JKStatement() {
    abstract var field: JKAssignableExpression
    abstract var expression: JKExpression
    abstract var operator: JKOperator
}

interface JKKtCall : JKMethodCallExpression

interface JKKtMethodCallExpression : JKMethodCallExpression

interface JKKtAlsoCallExpression : JKKtMethodCallExpression {
    var statement: JKStatement
}

interface JKKtLiteralExpression : JKLiteralExpression

abstract class JKKtWhenStatement : JKStatement() {
    abstract var expression: JKExpression
    abstract var cases: List<JKKtWhenCase>
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

interface JKKtIsExpression : JKExpression, PsiOwner {
    var expression: JKExpression
    var type: JKTypeElement
}

abstract class JKKtInitDeclaration : JKDeclaration() {
    abstract var block: JKBlock
}


abstract class JKKtConvertedFromForLoopSyntheticWhileStatement : JKStatement() {
    abstract var variableDeclaration: JKStatement
    abstract var whileStatement: JKWhileStatement
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

interface JKKtAnnotationArrayInitializerExpression : JKExpression, JKBranchElement {
    val initializers: List<JKAnnotationMemberValue>
}
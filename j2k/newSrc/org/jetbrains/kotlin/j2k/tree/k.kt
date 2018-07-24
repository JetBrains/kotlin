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
        INTERNAL, LATEINIT, NOINLINE, OPEN, OPERATOR, OUT, OVERRIDE, REIFIED, SEALED, SUSPEND, TAILREC, VARARG, PRIVATE, PROTECTED
    }
}

interface JKKtMethodCallExpression : JKMethodCallExpression

interface JKKtAlsoCallExpression : JKKtMethodCallExpression {
    var statement: JKStatement
    val parameterName: String
}

interface JKKtLiteralExpression : JKLiteralExpression
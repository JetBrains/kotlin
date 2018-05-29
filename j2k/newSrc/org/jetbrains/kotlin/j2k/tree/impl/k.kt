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

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor

class JKKtPropertyImpl(
    modifierList: JKModifierList,
    type: JKTypeElement,
    name: JKNameIdentifier,
    initializer: JKExpression,
    getter: JKBlock,
    setter: JKBlock
) : JKBranchElementBase(), JKKtProperty {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtProperty(this, data)

    override var modifierList: JKModifierList by child(modifierList)
    override var type by child(type)
    override var name: JKNameIdentifier by child(name)
    override val initializer: JKExpression by child(initializer)
    override val getter: JKBlock by child(getter)
    override val setter: JKBlock by child(setter)
}

class JKKtFunctionImpl(
    returnType: JKTypeElement,
    name: JKNameIdentifier,
    valueArguments: List<JKValueArgument>,
    block: JKBlock,
    modifierList: JKModifierList
) : JKBranchElementBase(), JKKtFunction {

    override var returnType: JKTypeElement by child(returnType)
    override var name: JKNameIdentifier by child(name)
    override var valueArguments: List<JKValueArgument> by children(valueArguments)
    override var block: JKBlock by child(block)
    override var modifierList: JKModifierList by child(modifierList)
}

sealed class JKKtQualifierImpl : JKQualifier, JKElementBase() {
    object DOT : JKKtQualifierImpl()
    object SAFE : JKKtQualifierImpl()
}

class JKKtCallExpressionImpl(
    override val identifier: JKMethodSymbol,
    arguments: JKExpressionList
) : JKKtMethodCallExpression, JKBranchElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtMethodCallExpression(this, data)

    override var arguments: JKExpressionList by child(arguments)
}

class JKKtFieldAccessExpressionImpl(override val identifier: JKFieldSymbol) : JKKtFieldAccessExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtFieldAccessExpression(this, data)
}

class JKKtLiteralExpressionImpl(
    override val literal: String,
    override val type: JKLiteralExpression.LiteralType
) : JKKtLiteralExpression,
    JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtLiteralExpression(this, data)
}

sealed class JKKtOperatorImpl : JKOperator, JKElementBase() {
    object PLUS : JKKtOperatorImpl()
    object MINUS : JKKtOperatorImpl()
    object EQEQ : JKKtOperatorImpl()
    object NE : JKKtOperatorImpl()
}
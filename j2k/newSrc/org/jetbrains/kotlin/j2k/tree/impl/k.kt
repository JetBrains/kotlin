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
import org.jetbrains.kotlin.j2k.tree.visitors.JKTransformer
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor

class JKKtPropertyImpl(override var modifierList: JKModifierList,
                       override var type: JKType,
                       override var name: JKNameIdentifier,
                       override var initializer: JKExpression? = null,
                       override var getter: JKBlock? = null,
                       override var setter: JKBlock? = null) : JKElementBase(), JKKtProperty {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtProperty(this, data)
    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        modifierList.accept(visitor, data)
        type.accept(visitor, data)
        name.accept(visitor, data)
        initializer?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        modifierList = modifierList.transform(transformer, data)
        type = type.transform(transformer, data)
        name = name.transform(transformer, data)
        initializer = initializer?.transform(transformer, data)
        getter = getter?.transform(transformer, data)
        setter = setter?.transform(transformer, data)
    }

    override val valid: Boolean
        get() = true
}

class JKKtFunctionImpl(override var returnType: JKType,
                       override var name: JKNameIdentifier,
                       override var valueArguments: List<JKValueArgument>,
                       override var block: JKBlock?,
                       override var modifierList: JKModifierList) : JKElementBase(), JKKtFunction {
    override val valid: Boolean
        get() = true
}

sealed class JKKtQualificationIdentifierImpl : JKKtQualificationIdentifier, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtQualificationIdentifier(this, data)

    object DOT : JKKtQualificationIdentifierImpl()
    object SAFE : JKKtQualificationIdentifierImpl()
}

class JKKtCallExpressionImpl(override val identifier: JKMethodReference,
                                     override val arguments: JKExpressionList) : JKKtMethodCallExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtMethodCallExpression(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        identifier.accept(visitor, data)
        arguments.accept(visitor, data)
    }
}

class JKKtFieldAccessExpressionImpl(override val identifier: JKFieldReference) : JKKtFieldAccessExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtFieldAccessExpression(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        identifier.accept(visitor, data)
    }
}

class JKKtLiteralExpressionImpl(override val literal: String, override val type: JKLiteralExpression.LiteralType) : JKKtLiteralExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtLiteralExpression(this, data)
}

sealed class JKKtOperatorIdentifierImpl : JKKtOperatorIdentifier, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitKtOperatorIdentifier(this, data)

    object PLUS : JKKtOperatorIdentifierImpl()
    object MINUS : JKKtOperatorIdentifierImpl()
}
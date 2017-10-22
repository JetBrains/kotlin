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

class JKJavaFieldImpl(override var modifierList: JKModifierList,
                      override var type: JKTypeIdentifier,
                      override var name: JKNameIdentifier,
                      override var initializer: JKExpression?) : JKJavaField, JKElementBase() {

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaField(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D)
            = listOfNotNull(type, name, initializer).forEach { it.accept(visitor, data) }

    override fun <D> transformChildren(transformer: JKTransformer<D>, data: D) {
        type = type.transform(transformer, data)
        name = name.transform(transformer, data)
        initializer = initializer?.transform(transformer, data)
    }
}


class JKJavaTypeIdentifierImpl(override val typeName: String) : JKJavaTypeIdentifier, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaTypeIdentifier(this, data)
}


class JKJavaStringLiteralExpressionImpl(override val text: String) : JKJavaStringLiteralExpression, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaStringLiteralExpression(this, data)
}


class JKJavaAccessModifierImpl(override val type: JKJavaAccessModifier.AccessModifierType) : JKJavaAccessModifier, JKElementBase() {
    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaAccessModifier(this, data)
}
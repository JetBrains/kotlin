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

import org.jetbrains.kotlin.j2k.tree.visitors.JKTransformer
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor


interface JKElement {
    fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R

    fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D)

    fun <D> transform(transformer: JKTransformer<D>, data: D): JKElement

    fun <D> transformChildren(transformer: JKTransformer<D>, data: D)
}

interface JKClass : JKDeclaration {
    var modifierList: JKModifierList
    var name: JKNameIdentifier
    val declarations: MutableList<JKDeclaration>
}

interface JKStatement : JKElement

interface JKExpression : JKStatement

interface JKLoop : JKStatement

interface JKDeclaration : JKElement

interface JKBlock : JKElement

interface JKCall : JKExpression

interface JKIdentifier : JKElement

interface JKTypeIdentifier : JKIdentifier

interface JKNameIdentifier : JKIdentifier {
    val name: String
}

interface JKLiteralExpression : JKExpression

interface JKModifierList : JKElement {
    val modifiers: MutableList<JKModifier>
}

interface JKModifier : JKElement

interface JKAccessModifier : JKModifier
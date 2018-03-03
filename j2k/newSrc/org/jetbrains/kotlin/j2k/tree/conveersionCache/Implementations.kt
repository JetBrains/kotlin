/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k.tree.conveersionCache

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKElementBase
import org.jetbrains.kotlin.j2k.tree.impl.JKJavaTypeIdentifierImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKModifierListImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKReferenceType
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor


class JKMultiverseField(override var name: JKNameIdentifier) : JKMultiverseDeclaration, JKJavaField {
    override var initializer: JKExpression? = null
    override var modifierList: JKModifierList = JKModifierListImpl()
    override var type: JKTypeIdentifier = JKJavaTypeIdentifierImpl("") //TODO
    override val valid: Boolean
        get() = true
}

class JKMultiverseProperty : JKMultiverseDeclaration {
    override val valid: Boolean
        get() = true
}

class JKMultiverseMethod(override var name: JKNameIdentifier) : JKMultiverseDeclaration, JKMethod {
    override var modifierList: JKModifierList = JKModifierListImpl()
    override var block: JKBlock? = null
    override var valueArguments: List<JKValueArgument> = listOf()
    override val valid: Boolean
        get() = true
}

class JKMultiverseClass(override var name: JKNameIdentifier, override var declarations: List<JKDeclaration>,
                        override var classKind: JKClass.ClassKind, override var modifierList: JKModifierList) : JKClass, JKMultiverseDeclaration {
    override val valid: Boolean
        get() = true
}

class JKMultiverseClassReferenceImpl(override val target: JKClass) : JKJavaClassReference, JKElementBase() {
    override val referenceType: JKReferenceType = JKReferenceType.M2U

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitJavaClassReference(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {

    }
}
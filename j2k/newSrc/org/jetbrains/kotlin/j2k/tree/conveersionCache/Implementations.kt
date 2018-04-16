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
import org.jetbrains.kotlin.j2k.tree.impl.JKJavaPrimitiveTypeImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKModifierListImpl

abstract class JKMultiverseElementBase : JKTreeElement {
    override var parent: JKTreeElement? = null

    fun <D : JKElement> D.setParent(p: JKElement): D {
        parent = p
        return this
    }
}

class JKMultiverseField(override var name: JKNameIdentifier) : JKField, JKMultiverseElementBase() {
    override var modifierList: JKModifierList = JKModifierListImpl()
    override var type: JKType = JKJavaPrimitiveTypeImpl.BOOLEAN //TODO
    override val valid: Boolean
        get() = true
}

class JKMultiverseMethod(override var name: JKNameIdentifier) : JKMethod, JKMultiverseElementBase() {
    override val returnType: JKType
        get() = TODO("not implemented")
    override var modifierList: JKModifierList = JKModifierListImpl()
    override var valueArguments: List<JKValueArgument> = listOf()
    override val valid: Boolean
        get() = true
}

class JKMultiverseClass(
    override var name: JKNameIdentifier, override var declarations: List<JKDeclaration>,
    override var classKind: JKClass.ClassKind, override var modifierList: JKModifierList
) : JKClass, JKMultiverseElementBase() {
    override val valid: Boolean
        get() = true
}
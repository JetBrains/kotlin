/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k.ast

import org.jetbrains.kotlin.j2k.AccessorKind
import org.jetbrains.kotlin.j2k.CodeBuilder
import org.jetbrains.kotlin.j2k.getDefaultInitializer

class Property(
        val identifier: Identifier,
        annotations: Annotations,
        modifiers: Modifiers,
        val isVar: Boolean,
        val type: Type,
        val explicitType: Boolean,
        private val initializer: DeferredElement<Expression>,
        private val needInitializer: Boolean,
        private val getter: PropertyAccessor?,
        private val setter: PropertyAccessor?,
        private val isInInterface: Boolean
) : Member(annotations, modifiers) {

    private fun presentationModifiers(): Modifiers {
        var modifiers = this.modifiers
        if (isInInterface) {
            modifiers = modifiers.without(Modifier.ABSTRACT)
        }

        if (modifiers.contains(Modifier.OVERRIDE)) {
            modifiers = modifiers.filter { it != Modifier.OPEN }
        }

        return modifiers
    }

    override fun generateCode(builder: CodeBuilder) {
        builder.append(annotations)
                .appendWithSpaceAfter(presentationModifiers())
                .append(if (isVar) "var " else "val ")
                .append(identifier)

        if (explicitType) {
            builder append ":" append type
        }

        var initializerToUse: Element = initializer
        if (initializerToUse.isEmpty && needInitializer) {
            initializerToUse = getDefaultInitializer(this) ?: Element.Empty
        }
        if (!initializerToUse.isEmpty) {
            builder append " = " append initializerToUse
        }

        if (getter != null) {
            builder append "\n" append getter
        }

        if (setter != null) {
            builder append "\n" append setter
        }
    }
}

class PropertyAccessor(
        private val kind: AccessorKind,
        annotations: Annotations,
        modifiers: Modifiers,
        parameterList: ParameterList?,
        body: DeferredElement<Block>?
) : FunctionLike(annotations, modifiers, parameterList, body) {

    override fun generateCode(builder: CodeBuilder) {
        builder.append(annotations)

        builder.appendWithSpaceAfter(presentationModifiers())

        when (kind) {
            AccessorKind.GETTER -> builder.append("get")
            AccessorKind.SETTER -> builder.append("set")
        }

        if (parameterList != null) {
            builder.append(parameterList)
        }

        if (body != null) {
            builder.append(" ").append(body)
        }
    }
}

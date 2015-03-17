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

import org.jetbrains.kotlin.j2k.*

class Function(
        val name: Identifier,
        annotations: Annotations,
        modifiers: Modifiers,
        val returnType: Type,
        val typeParameterList: TypeParameterList,
        val parameterList: ParameterList,
        val body: DeferredElement<Block>?,
        val isInTrait: Boolean
) : Member(annotations, modifiers) {

    private fun presentationModifiers(): Modifiers {
        var modifiers = this.modifiers
        if (isInTrait) {
            modifiers = modifiers.without(Modifier.ABSTRACT)
        }

        if (modifiers.contains(Modifier.OVERRIDE)) {
            modifiers = modifiers.filter { it != Modifier.OPEN && it !in ACCESS_MODIFIERS }
        }

        return modifiers
    }

    override fun generateCode(builder: CodeBuilder) {
        builder.append(annotations)
                .appendWithSpaceAfter(presentationModifiers())
                .append("fun ")
                .appendWithSuffix(typeParameterList, " ")
                .append(name)
                .append("(")
                .append(parameterList)
                .append(")")

        if (!returnType.isUnit()) {
            builder append ":" append returnType
        }

        typeParameterList.appendWhere(builder)

        if (body != null) {
            builder append " " append body
        }
    }
}

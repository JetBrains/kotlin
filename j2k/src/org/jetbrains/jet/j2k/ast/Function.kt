/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.ast

import java.util.ArrayList
import org.jetbrains.jet.j2k.*

open class Function(
        val converter: Converter,
        val name: Identifier,
        annotations: Annotations,
        modifiers: Set<Modifier>,
        val `type`: Type,
        val typeParameterList: TypeParameterList,
        val parameterList: ParameterList,
        var block: Block?,
        val isInTrait: Boolean
) : Member(annotations, modifiers) {

    private fun CodeBuilder.appendModifiers(): CodeBuilder {
        val resultingModifiers = ArrayList<Modifier>()
        val isOverride = modifiers.contains(Modifier.OVERRIDE)
        if (isOverride) {
            resultingModifiers.add(Modifier.OVERRIDE)
        }

        val accessModifier = modifiers.accessModifier()
        if (accessModifier != null && !isOverride) {
            resultingModifiers.add(accessModifier)
        }

        if (modifiers.contains(Modifier.ABSTRACT) && !isInTrait) {
            resultingModifiers.add(Modifier.ABSTRACT)
        }

        if (modifiers.contains(Modifier.OPEN) && !isOverride) {
            resultingModifiers.add(Modifier.OPEN)
        }

        return append(resultingModifiers)
    }

    override fun generateCode(builder: CodeBuilder) {
        builder.append(annotations)
                .appendModifiers()
                .append(" fun ")
                .appendWithSuffix(typeParameterList, " ")
                .append(name)
                .append("(")
                .append(parameterList)
                .append(")")

        if (!`type`.isUnit()) {
            builder append ":" append `type`
        }

        typeParameterList.appendWhere(builder)

        if (block != null) {
            builder append " " append block!!
        }
    }
}

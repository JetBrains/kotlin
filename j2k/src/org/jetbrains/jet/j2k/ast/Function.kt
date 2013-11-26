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

import org.jetbrains.jet.j2k.ast.types.Type
import java.util.ArrayList
import org.jetbrains.jet.j2k.ast.types.isUnit
import org.jetbrains.jet.j2k.Converter

public open class Function(val converter: Converter,
                           val name: Identifier,
                           val docComments: List<Node>,
                           modifiers: Set<Modifier>,
                           val `type`: Type,
                           val typeParameters: List<Element>,
                           val params: Element,
                           var block: Block?) : Member(modifiers) {

    private fun typeParametersToKotlin() = when {
        !typeParameters.isEmpty() -> typeParameters.map { it.toKotlin() }.makeString(", ", "<", "> ")
        else -> ""
    }


    private fun hasWhere(): Boolean = typeParameters.any { it is TypeParameter && it.hasWhere() }

    private fun typeParameterWhereToKotlin(): String {
        if (hasWhere())
        {
            val wheres = typeParameters.filter { it is TypeParameter }.map { ((it as TypeParameter).getWhereToKotlin() ) }
            return "where " + wheres.makeString(", ") + " "
        }

        return ""
    }

    private fun modifiersToKotlin(): String {
        val resultingModifiers = ArrayList<Modifier>()
        val isOverride = modifiers.contains(Modifier.OVERRIDE)
        if (isOverride) {
            resultingModifiers.add(Modifier.OVERRIDE)
        }

        val accessModifier = accessModifier()
        if (accessModifier != null && !isOverride) {
            resultingModifiers.add(accessModifier)
        }

        if (isAbstract()) {
            resultingModifiers.add(Modifier.ABSTRACT)
        }

        if (converter.settings.openByDefault &&
        !modifiers.contains(Modifier.ABSTRACT) &&
        !isOverride &&
        !modifiers.contains(Modifier.FINAL) &&
        !modifiers.contains(Modifier.PRIVATE)) {
            resultingModifiers.add(Modifier.OPEN)
        }

        if (modifiers.contains(Modifier.NOT_OPEN)) {
            resultingModifiers.remove(Modifier.OPEN)
        }

        return resultingModifiers.toKotlin()
    }

    private fun returnTypeToKotlin() = if (!`type`.isUnit()) " : " + `type`.toKotlin() + " " else " "

    public override fun toKotlin(): String {
        return docComments.toKotlin("\n", "", "\n") +
        modifiersToKotlin() +
        "fun ${typeParametersToKotlin()}${name.toKotlin()}" +
        "(${params.toKotlin()})" +
        returnTypeToKotlin() +
        typeParameterWhereToKotlin() +
        block?.toKotlin()
    }
}

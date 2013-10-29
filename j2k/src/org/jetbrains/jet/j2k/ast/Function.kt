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

public open class Function(val name: Identifier,
                           val docComments: List<Node>,
                           modifiers: Set<Modifier>,
                           val `type`: Type,
                           val typeParameters: List<Element>,
                           val params: Element,
                           var block: Block?) : Member(modifiers) {
    private fun typeParametersToKotlin(): String {
        return (if (typeParameters.size() > 0)
            "<" + typeParameters.map { it.toKotlin() }.makeString(", ") + ">"
        else
            "")
    }

    private fun hasWhere(): Boolean = typeParameters.any { it is TypeParameter && it.hasWhere() }

    private fun typeParameterWhereToKotlin(): String {
        if (hasWhere())
        {
            val wheres = typeParameters.filter { it is TypeParameter }.map { ((it as TypeParameter).getWhereToKotlin() ) }
            return " where " + wheres.makeString(", ") + " "
        }

        return ""
    }

    open fun modifiersToKotlin(): String {
        val modifierList = ArrayList<Modifier>()
        val accessModifier = accessModifier()
        if (accessModifier != null) {
            modifierList.add(accessModifier)
        }

        if (isAbstract()) {
            modifierList.add(Modifier.ABSTRACT)
        }

        if (modifiers.contains(Modifier.OVERRIDE)) {
            modifierList.add(Modifier.OVERRIDE)
        }

        if (!modifiers.contains(Modifier.ABSTRACT) &&
        !modifiers.contains(Modifier.OVERRIDE) &&
        !modifiers.contains(Modifier.FINAL) &&
        !modifiers.contains(Modifier.PRIVATE)) {
            modifierList.add(Modifier.OPEN)
        }

        if (modifiers.contains(Modifier.NOT_OPEN)) {
            modifierList.remove(Modifier.OPEN)
        }

        return modifierList.toKotlin()
    }

    public override fun toKotlin(): String {
        return docComments.toKotlin("\n", "", "\n") +
        modifiersToKotlin() +
        "fun " + name.toKotlin() +
        typeParametersToKotlin() +
        "(" + params.toKotlin() + ") : " +
        `type`.toKotlin() + " " + typeParameterWhereToKotlin() +
        block?.toKotlin()
    }
}

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
import org.jetbrains.jet.j2k.Converter

open class Function(
        val converter: Converter,
        val name: Identifier,
        comments: MemberComments,
        annotations: Annotations,
        modifiers: Set<Modifier>,
        val `type`: Type,
        val typeParameterList: TypeParameterList,
        val parameterList: ParameterList,
        var block: Block?,
        val isInTrait: Boolean
) : Member(comments, annotations, modifiers) {

    private fun modifiersToKotlin(): String {
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

        return resultingModifiers.toKotlin()
    }

    private fun returnTypeToKotlin() = if (!`type`.isUnit()) " : " + `type`.toKotlin() + " " else " "

    override fun toKotlin(): String {
        return commentsToKotlin() +
                annotations.toKotlin() +
                modifiersToKotlin() +
                "fun ${typeParameterList.toKotlin().withSuffix(" ")}${name.toKotlin()}" +
                "(${parameterList.toKotlin()})" +
                returnTypeToKotlin() +
                typeParameterList.whereToKotlin() +
                block?.toKotlin()
    }
}

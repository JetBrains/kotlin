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

import org.jetbrains.jet.j2k.CommentsAndSpaces

abstract class Member(val annotations: Annotations, val modifiers: Set<Modifier>) : Element()

class ClassBody (
        val primaryConstructor: PrimaryConstructor?,
        val secondaryConstructors: List<SecondaryConstructor>,
        val normalMembers: List<Member>,
        val classObjectMembers: List<Member>,
        val lBrace: LBrace,
        val rBrace: RBrace) {

    fun toKotlin(containingClass: Class?, commentsAndSpaces: CommentsAndSpaces): String {
        val builder = StringBuilder()
        builder.append(normalMembers.toKotlin(commentsAndSpaces, "\n"))

        val primaryConstructor = primaryConstructorBodyToKotlin(commentsAndSpaces)
        if (primaryConstructor.isNotEmpty() && builder.length() > 0) {
            builder.append("\n\n") // blank line before constructor body
        }
        builder.append(primaryConstructor)

        val classObject = classObjectToKotlin(containingClass, commentsAndSpaces)
        if (classObject.isNotEmpty() && builder.length() > 0) {
            builder.append("\n\n") // blank line before class object
        }
        builder.append(classObject)

        return if (builder.length() > 0)
            " " + lBrace.toKotlin(commentsAndSpaces) + "\n" + builder + "\n" + rBrace.toKotlin(commentsAndSpaces)
        else
            ""
    }

    private fun primaryConstructorBodyToKotlin(commentsAndSpaces: CommentsAndSpaces): String {
        val constructor = primaryConstructor
        if (constructor == null || constructor.block?.isEmpty ?: true) return ""
        return constructor.bodyToKotlin(commentsAndSpaces)
    }

    private fun classObjectToKotlin(containingClass: Class?, commentsAndSpaces: CommentsAndSpaces): String {
        if (secondaryConstructors.isEmpty() && classObjectMembers.isEmpty()) return ""
        val factoryFunctions = secondaryConstructors.map { it.toFactoryFunction(containingClass) }
        return "class object {\n" + (factoryFunctions + classObjectMembers).toKotlin(commentsAndSpaces, "\n") + "\n}"
    }
}

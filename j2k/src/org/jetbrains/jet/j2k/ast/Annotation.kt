/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

class Annotation(val name: Identifier, val arguments: List<Pair<Identifier?, Expression>>, val brackets: Boolean) : Element() {
    private fun surroundWithBrackets(text: String) = if (brackets) "[$text]" else text

    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String {
        if (arguments.isEmpty()) {
            return surroundWithBrackets(name.toKotlin(commentsAndSpaces))
        }

        val argsText = arguments.map {
            if (it.first != null)
                it.first!!.toKotlin(commentsAndSpaces) + " = " + it.second.toKotlin(commentsAndSpaces)
            else
                it.second.toKotlin(commentsAndSpaces)
        }.makeString(", ")
        return surroundWithBrackets(name.toKotlin(commentsAndSpaces) + "(" + argsText + ")")
    }
}

class Annotations(val annotations: List<Annotation>, val newLines: Boolean) : Element() {
    private val br = if (newLines) "\n" else " "

    override fun toKotlinImpl(commentsAndSpaces: CommentsAndSpaces): String {
        return if (annotations.isNotEmpty()) annotations.map { it.toKotlin(commentsAndSpaces) }.makeString(br) + br else ""
    }

    fun plus(other: Annotations) = Annotations(annotations + other.annotations, newLines || other.newLines)

    class object {
        val Empty = Annotations(listOf(), false)
    }
}

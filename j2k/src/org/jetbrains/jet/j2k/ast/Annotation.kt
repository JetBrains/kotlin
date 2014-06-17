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

import java.util.HashSet

class Annotation(val name: Identifier, val arguments: List<Pair<Identifier?, Expression>>, val brackets: Boolean) : Element() {
    private fun surroundWithBrackets(text: String) = if (brackets) "[$text]" else text

    override fun toKotlin(): String {
        if (arguments.isEmpty()) {
            return surroundWithBrackets(name.toKotlin())
        }

        val argsText = arguments.map {
            if (it.first != null)
                it.first!!.toKotlin() + " = " + it.second.toKotlin()
            else
                it.second.toKotlin()
        }.makeString(", ")
        return surroundWithBrackets(name.toKotlin() + "(" + argsText + ")")
    }
}

class Annotations(val annotations: List<Annotation>, val newLines: Boolean) {
    private val br = if (newLines) "\n" else " "

    fun toKotlin(): String = if (annotations.isNotEmpty()) annotations.map { it.toKotlin() }.makeString(br) + br else ""

    fun plus(other: Annotations) = Annotations(annotations + other.annotations, newLines || other.newLines)

    class object {
        val Empty = Annotations(listOf(), false)
    }
}

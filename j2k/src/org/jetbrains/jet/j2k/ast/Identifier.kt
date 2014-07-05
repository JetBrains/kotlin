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

import org.jetbrains.jet.j2k.CodeBuilder
import com.intellij.psi.PsiNameIdentifierOwner

fun PsiNameIdentifierOwner.declarationIdentifier(): Identifier {
    val name = getName()
    return if (name != null) Identifier(name, false).assignPrototype(getNameIdentifier()!!) else Identifier.Empty
}

class Identifier(
        val name: String,
        override val isNullable: Boolean = true,
        private val quotingNeeded: Boolean = true
) : Expression() {

    override val isEmpty: Boolean
        get() = name.isEmpty()

    private fun toKotlin(): String {
        if (quotingNeeded && ONLY_KOTLIN_KEYWORDS.contains(name) || name.contains("$")) {
            return quote(name)
        }

        return name
    }

    override fun generateCode(builder: CodeBuilder) {
        builder.append(toKotlin())
    }

    private fun quote(str: String): String = "`" + str + "`"

    override fun toString() = if (isNullable) "$name?" else name

    class object {
        val Empty = Identifier("")

        val ONLY_KOTLIN_KEYWORDS: Set<String> = setOf(
                "package", "as", "type", "val", "var", "fun", "is", "in", "object", "when", "trait", "This"
        )

        fun toKotlin(name: String): String = Identifier(name).toKotlin()
    }
}

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

enum class Modifier(val name: String) {
    PUBLIC: Modifier("public")
    PROTECTED: Modifier("protected")
    PRIVATE: Modifier("private")
    ABSTRACT: Modifier("abstract")
    OPEN: Modifier("open")
    OVERRIDE: Modifier("override")
    INNER: Modifier("inner")

    public fun toKotlin(): String = name
}

val ACCESS_MODIFIERS = setOf(Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE)

fun Collection<Modifier>.accessModifier(): Modifier? {
    return firstOrNull { ACCESS_MODIFIERS.contains(it) }
}

fun Collection<Modifier>.toKotlin(): String
        = if (isNotEmpty()) map { it.toKotlin() }.makeString(" ") + " " else ""



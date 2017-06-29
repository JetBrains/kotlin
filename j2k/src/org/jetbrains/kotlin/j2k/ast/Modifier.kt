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

import org.jetbrains.kotlin.j2k.CodeBuilder

enum class Modifier(private val str: String) {
    PUBLIC("public"),
    PROTECTED("protected"),
    PRIVATE("private"),
    INTERNAL("internal"),
    ANNOTATION("annotation"),
    ABSTRACT("abstract"),
    OPEN("open"),
    OVERRIDE("override"),
    EXTERNAL("external"),
    CONST("const"),
    INNER("inner");

    fun toKotlin(): String = str
}

val ACCESS_MODIFIERS = setOf(Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE, Modifier.INTERNAL)

class Modifiers(modifiers: Collection<Modifier>) : Element() {
    val modifiers = modifiers.toSet()

    override fun generateCode(builder: CodeBuilder) {
        val modifiersToInclude = if (modifiers.contains(Modifier.OVERRIDE))
            modifiers // for override members we remove redundant visibility modifiers in post-processing
        else
            modifiers.filter { it != Modifier.PUBLIC }
        val text = modifiersToInclude
                .sortedBy { it.ordinal }
                .joinToString(" ") { it.toKotlin() }
        builder.append(text)
    }

    override val isEmpty: Boolean
        get() = modifiers.isEmpty()

    fun with(modifier: Modifier?): Modifiers = if (modifier != null) Modifiers(modifiers + listOf(modifier)).assignPrototypesFrom(this) else this

    fun without(modifier: Modifier?): Modifiers {
        if (modifier == null || !modifiers.contains(modifier)) return this
        return Modifiers(modifiers.filter { it != modifier }).assignPrototypesFrom(this)
    }

    fun contains(modifier: Modifier): Boolean = modifiers.contains(modifier)

    val isPrivate: Boolean get() = contains(Modifier.PRIVATE)

    fun accessModifier(): Modifier? = modifiers.firstOrNull { it in ACCESS_MODIFIERS }

    operator fun plus(other: Modifiers): Modifiers {
        return Modifiers(this.modifiers + other.modifiers).assignPrototypesFrom(this)
    }

    companion object {
        val Empty = Modifiers(listOf())
    }
}

fun Modifiers.filter(predicate: (Modifier) -> Boolean): Modifiers
        = Modifiers(modifiers.filter(predicate)).assignPrototypesFrom(this)

infix fun CodeBuilder.appendWithSpaceAfter(modifiers: Modifiers): CodeBuilder {
    if (!modifiers.isEmpty) {
        this append modifiers append " "
    }
    return this
}


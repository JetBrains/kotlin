/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.util

import org.jetbrains.kotlin.js.common.makeValidES5Identifier
import org.jetbrains.kotlin.utils.addToStdlib.joinToWithBuffer

abstract class NameScope {
    abstract fun isReserved(name: String): Boolean

    object EmptyScope : NameScope() {
        override fun isReserved(name: String): Boolean = false
    }
}

/**
 * A map from [T] values to strings with an additional property that all the associated strings are made unique by appending a number
 * for disambiguation, if needed.
 */
class NameTable<T>(private val parent: NameScope = EmptyScope) : NameScope() {
    private val reserved = hashSetOf<String>()
    private val names = hashMapOf<T, String>()

    private val suggestedNameLastIdx = mutableMapOf<String, Int>()

    operator fun get(name: T): String? = names[name]

    fun dump(renderKey: (T) -> String): String = buildString {
        appendLine("Names:")
        names.entries.joinToWithBuffer(this, separator = "\n") { (declaration, name) ->
            append("--- ")
            append(renderKey(declaration))
            append(" => ")
            append(name)
        }
    }

    override fun isReserved(name: String): Boolean {
        return parent.isReserved(name) || name in reserved
    }

    /**
     * Associates [name] with [declaration] as is, without disambiguation.
     */
    fun declareStableName(declaration: T, name: String) {
        names[declaration] = name
        reserved.add(name)
    }

    /**
     * Associates a name with [declaration] using [suggestedName] as the basis and optionally appending a number to it, if there already
     * is such a string in the map.
     *
     * @return The associated name.
     */
    fun declareFreshName(declaration: T, suggestedName: String): String {
        val freshName = findFreshName(makeValidES5Identifier(suggestedName))
        declareStableName(declaration, freshName)
        return freshName
    }

    private fun findFreshName(suggestedName: String): String {
        if (!isReserved(suggestedName))
            return suggestedName

        var i = suggestedNameLastIdx[suggestedName] ?: 0

        fun freshName() =
            suggestedName + "_" + i

        while (isReserved(freshName())) {
            i++
        }

        suggestedNameLastIdx[suggestedName] = i

        return freshName()
    }
}

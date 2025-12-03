/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.util

import org.jetbrains.kotlin.js.common.makeValidES5Identifier

abstract class NameScope {
    abstract fun isReserved(name: String): Boolean

    object EmptyScope : NameScope() {
        override fun isReserved(name: String): Boolean = false
    }
}

class NameTable<T>(
    val parent: NameScope = EmptyScope,
    val reserved: MutableSet<String> = hashSetOf(),
) : NameScope() {
    val names = mutableMapOf<T, String>()

    private val suggestedNameLastIdx = mutableMapOf<String, Int>()

    override fun isReserved(name: String): Boolean {
        return parent.isReserved(name) || name in reserved
    }

    fun declareStableName(declaration: T, name: String) {
        names[declaration] = name
        reserved.add(name)
    }

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

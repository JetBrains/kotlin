/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

interface Interner {
    fun <T> intern(value: T): T

    companion object {
        fun default(): Interner = DefaultInterner()
        fun none(): Interner = NoneInterner
    }
}

internal fun <T> Interner.internSet(set: Set<T>): Set<T> {
    return if (set.isEmpty()) return emptySet()
    else intern(set.map { intern(it) }.toSet())
}

internal fun <T> Interner.internList(list: List<T>): List<T> {
    return if (list.isEmpty()) emptyList()
    else intern(list.map { intern(it) })
}

private class DefaultInterner : Interner {
    private val values = mutableMapOf<Any, Any>()
    override fun <T> intern(value: T): T {
        if (value == null) return value

        @Suppress("unchecked_cast")
        return values.getOrPut(value) { value } as T
    }
}

object NoneInterner : Interner {
    override fun <T> intern(value: T): T = value
}

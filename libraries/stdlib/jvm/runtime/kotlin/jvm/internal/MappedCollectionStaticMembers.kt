/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import kotlin.internal.InlineOnly

@SinceKotlin("2.5")
internal class ListStaticMembers {
    companion {
        @SinceKotlin("2.5")
        @InlineOnly
        inline fun <E> of(): List<E> = emptyList()

        @SinceKotlin("2.5")
        @InlineOnly
        inline fun <E> of(element: E): List<E> = listOf(element)

        @SinceKotlin("2.5")
        fun <E> of(vararg elements: E): List<E> = if (elements.size > 0) elements.asList() else emptyList()
    }
}

@SinceKotlin("2.5")
internal class MutableListStaticMembers {
    companion {
        @SinceKotlin("2.5")
        @InlineOnly
        inline fun <E> of(): MutableList<E> = mutableListOf()

        @SinceKotlin("2.5")
        fun <E> of(vararg elements: E): MutableList<E> = if (elements.size == 0) ArrayList() else elements.asArrayList()
    }
}

@SinceKotlin("2.5")
internal class SetStaticMembers {
    companion {
        @SinceKotlin("2.5")
        @InlineOnly
        inline fun <E> of(): Set<E> = setOf()

        @SinceKotlin("2.5")
        @InlineOnly
        inline fun <E> of(element: E): Set<E> = setOf(element)

        @SinceKotlin("2.5")
        fun <E> of(vararg elements: E): Set<E> = elements.toSet()
    }
}

@SinceKotlin("2.5")
internal class MutableSetStaticMembers {
    companion {
        @SinceKotlin("2.5")
        @InlineOnly
        inline fun <E> of(): MutableSet<E> = mutableSetOf()

        @SinceKotlin("2.5")
        fun <E> of(vararg elements: E): MutableSet<E> = elements.toCollection(LinkedHashSet(mapCapacity(elements.size)))
    }
}

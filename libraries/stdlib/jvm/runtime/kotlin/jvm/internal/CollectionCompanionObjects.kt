/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package kotlin.jvm.internal

@ExperimentalStdlibApi
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object ListCompanionObject {
    @ExperimentalStdlibApi
    operator fun <T> of(): List<T> = listOf()

    @ExperimentalStdlibApi
    operator fun <T> of(element: T): List<T> = listOf(element)

    @ExperimentalStdlibApi
    operator fun <T> of(vararg elements: T): List<T> = listOf(*elements)
}

@ExperimentalStdlibApi
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object MutableListCompanionObject {
    @ExperimentalStdlibApi
    operator fun <T> of(): MutableList<T> = mutableListOf()

    @ExperimentalStdlibApi
    operator fun <T> of(element: T): MutableList<T> = mutableListOf(element)

    @ExperimentalStdlibApi
    operator fun <T> of(vararg elements: T): MutableList<T> = mutableListOf(*elements)
}

@ExperimentalStdlibApi
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object SetCompanionObject {
    @ExperimentalStdlibApi
    operator fun <T> of(): Set<T> = setOf()

    @ExperimentalStdlibApi
    operator fun <T> of(element: T): Set<T> = setOf(element)

    @ExperimentalStdlibApi
    operator fun <T> of(vararg elements: T): Set<T> = setOf(*elements)
}

@ExperimentalStdlibApi
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object MutableSetCompanionObject {
    @ExperimentalStdlibApi
    operator fun <T> of(): MutableSet<T> = mutableSetOf()

    @ExperimentalStdlibApi
    operator fun <T> of(element: T): MutableSet<T> = mutableSetOf(element)

    @ExperimentalStdlibApi
    operator fun <T> of(vararg elements: T): MutableSet<T> = mutableSetOf(*elements)
}

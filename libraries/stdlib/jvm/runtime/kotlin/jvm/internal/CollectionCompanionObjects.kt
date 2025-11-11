/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package kotlin.jvm.internal

@ExperimentalCollectionLiterals
@SinceKotlin("2.3")
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object ListCompanionObject {
    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    operator fun <T> of(): List<T> = listOf()

    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    operator fun <T> of(element: T): List<T> = listOf(element)

    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    operator fun <T> of(vararg elements: T): List<T> = listOf(*elements)
}

@ExperimentalCollectionLiterals
@SinceKotlin("2.3")
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object MutableListCompanionObject {
    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    operator fun <T> of(): MutableList<T> = mutableListOf()

    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    operator fun <T> of(element: T): MutableList<T> = mutableListOf(element)

    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    operator fun <T> of(vararg elements: T): MutableList<T> = mutableListOf(*elements)
}

@ExperimentalCollectionLiterals
@SinceKotlin("2.3")
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object SetCompanionObject {
    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    operator fun <T> of(): Set<T> = setOf()

    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    operator fun <T> of(element: T): Set<T> = setOf(element)

    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    operator fun <T> of(vararg elements: T): Set<T> = setOf(*elements)
}

@ExperimentalCollectionLiterals
@SinceKotlin("2.3")
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
internal object MutableSetCompanionObject {
    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    operator fun <T> of(): MutableSet<T> = mutableSetOf()

    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    operator fun <T> of(element: T): MutableSet<T> = mutableSetOf(element)

    @ExperimentalCollectionLiterals
    @SinceKotlin("2.3")
    operator fun <T> of(vararg elements: T): MutableSet<T> = mutableSetOf(*elements)
}

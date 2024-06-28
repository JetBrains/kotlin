/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.*

/**
 * A set of [Comparator] objects that could be used to sort collections of SIR nodes.
 */
object Comparators {
    val stableExtensionComparator: Comparator<SirExtension> = compareBy { it.extendedType.swift }
    val stableNamedComparator: Comparator<SirNamed> = compareBy { it.name }
    val stableVariableComparator: Comparator<SirVariable> = compareBy { it.name }
    val stableInitComparator: Comparator<SirInit> = compareBy(
        { it.parameters.size },
        { mangleParameters(it.parameters) },
    )
    val stableFunctionComparator: Comparator<SirFunction> = compareBy(
        { it.name },
        { it.parameters.size },
        { mangleParameters(it.parameters) },
    )

    val stableCallableComparator: Comparator<SirCallable> = compareBy<SirCallable> {
        when (it) {
            is SirInit -> 0
            is SirFunction -> 1
            is SirAccessor -> 2
        }
    }.thenComparing(stableInitComparator)
        .thenComparing(stableFunctionComparator)

    private fun mangleParameters(params: List<SirParameter>): String =
        params.joinToString { "${it.parameterName}-${it.argumentName}:${it.type.swift}" }

    private val SirType.swift
        get(): String = when (this) {
            is SirExistentialType -> error("Existential types are not supported yet")
            is SirNominalType -> type.name
            is SirErrorType -> "SirErrorType"
            is SirUnsupportedType -> "SirUnsupportedType"
        }

    private inline fun <T, reified R> Comparator<T>.thenComparing(comparator: Comparator<R>): Comparator<T> {
        return thenComparing { a, b ->
            if (a is R && b is R) comparator.compare(a, b) else 0
        }
    }
}
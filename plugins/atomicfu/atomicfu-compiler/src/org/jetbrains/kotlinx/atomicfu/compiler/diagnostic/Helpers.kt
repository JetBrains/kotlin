/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.name.ClassId

private const val KOTLINX_ATOMICFU = "kotlinx.atomicfu"
private val ATOMIC_TYPES = setOf(
    "AtomicInt",
    "AtomicLong",
    "AtomicBoolean",
    "AtomicRef",
    "AtomicIntArray",
    "AtomicLongArray",
    "AtomicBooleanArray",
    "AtomicArray"
)
private val ATOMIC_FACTORIES = setOf("atomic", "atomicArrayOfNulls")

internal fun ClassId.isAtomicType(): Boolean {
    return packageFqName.toString() == KOTLINX_ATOMICFU && relativeClassName.toString() in ATOMIC_TYPES
}

internal fun FirNamedReference.isAtomicFactory(): Boolean {
    if (symbol?.packageFqName()?.asString() != KOTLINX_ATOMICFU) return false
    val nameStr = name.asString()
    return nameStr in ATOMIC_TYPES || nameStr in ATOMIC_FACTORIES
}

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
private val ATOMIC_SCALAR_TYPES = setOf(
    "AtomicInt",
    "AtomicLong",
    "AtomicBoolean",
    "AtomicRef",
)
private val ATOMIC_ARRAY_TYPES = setOf(
    "AtomicIntArray",
    "AtomicLongArray",
    "AtomicBooleanArray",
    "AtomicArray"
)
private val ATOMIC_FACTORIES = setOf("atomic", "atomicArrayOfNulls")

/** Atomic scalar or array type */
internal fun ClassId.isAtomicType(): Boolean {
    if (packageFqName.toString() != KOTLINX_ATOMICFU) return false
    val className = relativeClassName.toString()
    return className in ATOMIC_SCALAR_TYPES || className in ATOMIC_ARRAY_TYPES
}

internal fun ClassId.isAtomicArrayType(): Boolean {
    if (packageFqName.toString() != KOTLINX_ATOMICFU) return false
    val className = relativeClassName.toString()
    return className in ATOMIC_ARRAY_TYPES
}

internal fun FirNamedReference.isAtomicFactory(): Boolean {
    if (symbol?.packageFqName()?.asString() != KOTLINX_ATOMICFU) return false
    val nameStr = name.asString()
    return nameStr in ATOMIC_SCALAR_TYPES || nameStr in ATOMIC_ARRAY_TYPES || nameStr in ATOMIC_FACTORIES
}

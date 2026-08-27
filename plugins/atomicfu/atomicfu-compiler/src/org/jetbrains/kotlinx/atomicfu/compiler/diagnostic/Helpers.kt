/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.atomicfu.compiler.backend.AtomicfuStandardClassIds

private val ATOMIC_SCALAR_TYPES = setOf(
    AtomicfuStandardClassIds.AtomicInt,
    AtomicfuStandardClassIds.AtomicLong,
    AtomicfuStandardClassIds.AtomicBoolean,
    AtomicfuStandardClassIds.AtomicRef
)
private val ATOMIC_ARRAY_TYPES = setOf(
    AtomicfuStandardClassIds.AtomicArray,
    AtomicfuStandardClassIds.AtomicIntArray,
    AtomicfuStandardClassIds.AtomicLongArray,
    AtomicfuStandardClassIds.AtomicBooleanArray
)
private val ATOMIC_FACTORIES: Set<Name> = setOf(Name.identifier("atomic"), Name.identifier("atomicArrayOfNulls"))
    .plus(ATOMIC_SCALAR_TYPES.map { it.shortClassName })
    .plus(ATOMIC_ARRAY_TYPES.map { it.shortClassName })

/** Atomic scalar or array type */
internal fun ClassId.isAtomicType(): Boolean = this in ATOMIC_SCALAR_TYPES || isAtomicArrayType()

internal fun ClassId.isAtomicArrayType(): Boolean = this in ATOMIC_ARRAY_TYPES

internal fun ClassId.isAtomicRefType(): Boolean = this == AtomicfuStandardClassIds.AtomicRef

internal fun ClassId.isAtomicRefArrayType(): Boolean = this == AtomicfuStandardClassIds.AtomicArray

internal fun FirNamedReference.isAtomicFactory(): Boolean {
    return symbol?.packageFqName() == AtomicfuStandardClassIds.BASE_ATOMICFU_PACKAGE && name in ATOMIC_FACTORIES
}

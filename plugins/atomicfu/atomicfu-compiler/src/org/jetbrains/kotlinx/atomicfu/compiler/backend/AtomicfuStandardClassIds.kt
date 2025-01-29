/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object AtomicfuStandardClassIds {
    val BASE_KOTLINX_PACKAGE = FqName("kotlinx")
    val BASE_ATOMICFU_PACKAGE = BASE_KOTLINX_PACKAGE.child(Name.identifier("atomicfu"))

    val AtomicBoolean = "AtomicBoolean".atomicsId()
    val AtomicInt = "AtomicInt".atomicsId()
    val AtomicLong = "AtomicLong".atomicsId()
    val AtomicRef = "AtomicRef".atomicsId()

    val atomicByPrimitive = mapOf(
        StandardClassIds.Boolean to AtomicBoolean,
        StandardClassIds.Int to AtomicInt,
        StandardClassIds.Long to AtomicLong,
    )

    val AtomicBooleanArray = "AtomicBooleanArray".atomicsId()
    val AtomicIntArray = "AtomicIntArray".atomicsId()
    val AtomicLongArray = "AtomicLongArray".atomicsId()
    val AtomicArray = "AtomicArray".atomicsId()

    val atomicArrayByPrimitive = mapOf(
        StandardClassIds.Boolean to AtomicBooleanArray,
        StandardClassIds.Int to AtomicIntArray,
        StandardClassIds.Long to AtomicLongArray,
    )

    object Callables {
        val atomic = "atomic".callableId(BASE_ATOMICFU_PACKAGE)

        val atomicRefCompareAndSet = "compareAndSet".callableId(AtomicRef)
        val atomicRefCompareAndExchange = "compareAndExchange".callableId(AtomicRef)
    }
}

private fun String.atomicsId() = ClassId(AtomicfuStandardClassIds.BASE_ATOMICFU_PACKAGE, Name.identifier(this))
private fun String.callableId(packageName: FqName) = CallableId(packageName, Name.identifier(this))
private fun String.callableId(classId: ClassId) = CallableId(classId, Name.identifier(this))

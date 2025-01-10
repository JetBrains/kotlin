/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object AtomicfuStandardClassIds {
    val BASE_KOTLINX_PACKAGE = FqName("kotlinx")
    val BASE_ATOMICFU_PACKAGE = BASE_KOTLINX_PACKAGE.child(Name.identifier("atomicfu"))

    val AtomicRef = "AtomicRef".atomicsId()

    object Callables {
        val atomic = "atomic".callableId(BASE_ATOMICFU_PACKAGE)
    }
}

private fun String.atomicsId() = ClassId(AtomicfuStandardClassIds.BASE_ATOMICFU_PACKAGE, Name.identifier(this))
private fun String.callableId(packageName: FqName) = CallableId(packageName, Name.identifier(this))

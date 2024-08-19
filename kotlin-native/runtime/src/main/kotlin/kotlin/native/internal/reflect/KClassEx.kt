/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.internal.reflect

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.InternalForKotlinNative
import kotlin.native.internal.KClassImpl
import kotlin.native.internal.NativePtr
import kotlin.native.internal.typeInfoPtr
import kotlin.reflect.KClass

/**
 * Returns super class of a given class, if any.
 *
 * Namely, given `class T: S` (where `S` is a `class`), `T::class.superClass == S::class`
 */
@InternalForKotlinNative
public val KClass<*>.superClass: KClass<*>?
    get() {
        val typeInfo = typeInfoPtr
        require(!typeInfo.isNull())
        val superTypeInfo = superClass(typeInfo)
        if (superTypeInfo.isNull())
            return null
        return KClassImpl<Any>(superTypeInfo)
    }

@GCUnsafeCall("Kotlin_native_internal_reflect_superClass")
private external fun superClass(typeInfo: NativePtr): NativePtr
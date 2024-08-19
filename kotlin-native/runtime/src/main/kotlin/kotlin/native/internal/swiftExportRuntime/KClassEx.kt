/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.internal.swiftExportRuntime

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.native.internal.InternalForKotlinNative
import kotlin.native.internal.typeInfoPtr
import kotlin.reflect.KClass

/**
 * Returns a function pointer for function to convert instances of T (where `this` is `T::class`)
 * to retained Swift objects.
 *
 * @see ToRetainedSwiftFunPtr
 */
@InternalForKotlinNative
public val KClass<*>.toRetainedSwiftFunPtr: ToRetainedSwiftFunPtr
    get() = ToRetainedSwiftFunPtr(typeInfoPtr)
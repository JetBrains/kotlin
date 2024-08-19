/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.internal.ref

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.InternalForKotlinNative
import kotlin.reflect.KClass

/**
 * Mark a function that is used to convert objects of type [target] to retained Swift objects.
 *
 * At most one function in the final binary may be bound to [target]
 */
@InternalForKotlinNative
@Target(AnnotationTarget.FUNCTION)
public annotation class ToRetainedSwift(val target: KClass<*>)

@ToRetainedSwift(Any::class)
@GCUnsafeCall("SwiftExport_kotlin_Any_toRetainedSwift")
private external fun anyToRetainedSwift(ref: ExternalRCRef): kotlin.native.internal.NativePtr
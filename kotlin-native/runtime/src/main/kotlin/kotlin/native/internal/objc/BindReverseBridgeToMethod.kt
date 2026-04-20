/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.internal.objc

import kotlin.native.internal.InternalForKotlinNative
import kotlin.reflect.KClass

/**
 * Mark that the annotated function is a reverse bridge for [targetMethod] on [targetClass].
 *
 * The annotated function will be used by the runtime to dispatch virtual calls from Kotlin
 * to Swift overrides. The compiler backend resolves [targetMethod] to the appropriate
 * vtable/itable slot and stores the annotated function's address as the reverse adapter.
 *
 * This annotation is placed on generated Kotlin bridge functions by Swift Export.
 */
@InternalForKotlinNative
@Target(AnnotationTarget.FUNCTION)
@Repeatable
@Retention(AnnotationRetention.BINARY)
public annotation class BindReverseBridgeToMethod(val targetClass: KClass<*>, val targetMethod: String)

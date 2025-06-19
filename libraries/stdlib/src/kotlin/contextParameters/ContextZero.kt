/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmName("ContextParametersKt")
@file:kotlin.jvm.JvmMultifileClass
package kotlin

/**
 * Runs the specified [block] with the given values in context scope.
 *
 * As opposed to [with], [context] only makes the values available for
 * context parameter resolution, but not as implicit receivers.
 *
 * @sample samples.misc.ContextParameters.useContext
 */
@kotlin.internal.InlineOnly
@SinceKotlin("2.2")
@Deprecated(level = DeprecationLevel.ERROR, message = "'context' requires at least one value")
public fun <R> context(block: () -> R): R = throw NotImplementedError()
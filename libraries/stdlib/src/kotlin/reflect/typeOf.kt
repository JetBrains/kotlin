/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

/**
 * Returns a runtime representation of the given reified type [T] as an instance of [KType].
 */
@SinceKotlin("1.6")
@WasExperimental(ExperimentalStdlibApi::class)
public inline fun <reified T> typeOf(): KType =
    throw UnsupportedOperationException("This function is implemented as an intrinsic on all supported platforms.")

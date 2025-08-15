/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

@Suppress("DEPRECATION_ERROR")
@PublishedApi
@SinceKotlin("2.3")
internal fun throwUninitializedPropertyAccessException(name: String): Nothing =
    throw UninitializedPropertyAccessException("lateinit property $name has not been initialized")

@PublishedApi
@SinceKotlin("2.3")
internal fun throwUnsupportedOperationException(message: String): Nothing =
    throw UnsupportedOperationException(message)

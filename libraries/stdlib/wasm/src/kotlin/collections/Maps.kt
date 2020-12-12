/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Calculate the initial capacity of a map.
 */
@PublishedApi
internal actual fun mapCapacity(expectedSize: Int): Int = TODO("Wasm stdlib: Maps")

/**
 * Checks a collection builder function capacity argument.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@PublishedApi
internal fun checkBuilderCapacity(capacity: Int) { TODO("Wasm stdlib: Maps") }
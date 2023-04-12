/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

import kotlin.experimental.ExperimentalNativeApi

/**
 * Throws an [AssertionError] if the [value] is false
 * and runtime assertions have been enabled during compilation.
 */
@Suppress("NOTHING_TO_INLINE")
@ExperimentalNativeApi
public inline fun assert(value: Boolean) {
    assert(value) { "Assertion failed" }
}

/**
 * Throws an [AssertionError] calculated by [lazyMessage] if the [value] is false
 * and runtime assertions have been enabled during compilation.
 */
@ExperimentalNativeApi
public inline fun assert(value: Boolean, lazyMessage: () -> Any) {
    if (!value) {
        val message = lazyMessage()
        throw AssertionError(message)
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

// TODO: Make this dependant on the compiler flag (like -ea on JVM)

/**
 * Throws an [AssertionError] if the [value] is false.
 */
@kotlin.internal.InlineOnly
public inline fun assert(value: Boolean) {
    assert(value) { "Assertion failed" }
}

/**
 * Throws an [AssertionError] calculated by [lazyMessage] if the [value] is false.
 */
@kotlin.internal.InlineOnly
public inline fun assert(value: Boolean, lazyMessage: () -> Any) {
    if (!value) {
        val message = lazyMessage()
        throw AssertionError(message)
    }
}

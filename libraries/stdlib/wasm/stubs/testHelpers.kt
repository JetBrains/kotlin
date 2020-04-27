/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.internal.OnlyInputTypes

public fun assert(x: Boolean) {
    if (!x) throw AssertionError("Assertion failed")
}

/** Asserts that the [expected] value is equal to the [actual] value, with an optional [message]. */
public fun <@OnlyInputTypes T> assertEquals(expected: T, actual: T, message: String? = null) {
    if (expected != actual) throw AssertionError("assertEquals failed: expected:$expected, actual:$actual, message=$message")
}

/** Asserts that the [actual] value is not equal to the illegal value, with an optional [message]. */
public fun <@OnlyInputTypes T> assertNotEquals(illegal: T, actual: T, message: String? = null) {
    if (illegal == actual) throw AssertionError("assertNotEquals failed: illegal:$illegal, actual:$actual, message=$message")
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

// This file plugs some declaration from "kotlin.test" library used in tests.

fun <T> assertEquals(a: T, b: T) {
    if (a != b) throw Exception("")
}

fun assertTrue(x: Boolean) {
    if (!x) throw Exception("")
}

fun assertFalse(x: Boolean) {
    if (x) throw Exception("")
}

@Suppress("UNUSED_PARAMETER")
fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
    if (actual == null) throw Exception("${if (message == null) "" else "$message. "}Expected value to be not null")
    return actual
}

@Suppress("UNUSED_PARAMETER")
fun assertNull(actual: Any?, message: String? = null) {
    if (actual != null) throw Exception("${if (message == null) "" else "$message. "}Expected value to be null, but was: <$actual>.")
}


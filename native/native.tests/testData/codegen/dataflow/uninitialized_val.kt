/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun foo(b: Boolean): Int {
    val x: Int
    if (b) {
        x = 1
    } else {
        x = 2
    }

    return x
}

fun box(): String {
    val uninitializedUnused: Int

    assertEquals(1, foo(true))
    assertEquals(2, foo(false))

    return "OK"
}
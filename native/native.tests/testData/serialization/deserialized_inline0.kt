/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun inline_todo() {
    try {
        TODO("OK")
    } catch (e: Throwable) {
        assertEquals("An operation is not implemented: OK", e.message)
    }
}

fun inline_maxof() {
    assertEquals(17, maxOf(10, 17))
    assertEquals(17, maxOf(17, 13))
    assertEquals(17, maxOf(17, 17))
}

fun inline_assert() {
    assert(true)
}

fun box(): String {
    inline_todo()
    inline_assert()
    inline_maxof()

    return "OK"
}


/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

@ThreadLocal
val xInt = 42

@ThreadLocal
val xString = "42"

@ThreadLocal
val xAny = Any()

fun box(): String {
    assertEquals(42, xInt)
    assertEquals("42", xString)
    assertTrue(xAny is Any)

    return "OK"
}

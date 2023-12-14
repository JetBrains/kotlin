/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

enum class E {
    E3,
    E1,
    E2
}

fun box(): String {
    assertEquals("E1", E.valueOf("E1").toString())
    assertEquals("E2", E.valueOf("E2").toString())
    assertEquals("E3", E.valueOf("E3").toString())
    assertEquals("E1", enumValueOf<E>("E1").toString())
    assertEquals("E2", enumValueOf<E>("E2").toString())
    assertEquals("E3", enumValueOf<E>("E3").toString())

    return "OK"
}
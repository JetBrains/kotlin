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
    assertEquals("E3", E.values()[0].toString())
    assertEquals("E1", E.values()[1].toString())
    assertEquals("E2", E.values()[2].toString())
    assertEquals("E3", enumValues<E>()[0].toString())
    assertEquals("E1", enumValues<E>()[1].toString())
    assertEquals("E2", enumValues<E>()[2].toString())

    return "OK"
}
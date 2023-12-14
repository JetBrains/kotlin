/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

enum class A(one: Int, val two: Int = one) {
    FOO(42)
}

fun box(): String {
    assertEquals(42, (A.FOO.two))
    return "OK"
}
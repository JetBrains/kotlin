/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

// Based on KT-27225.

value class First(val value: Int)

value class Second(val value: First) {
    constructor(c: Int) : this(First(c))
}

fun box(): String {
    assertEquals(Second(42).value.value, 42)
    return "OK"
}

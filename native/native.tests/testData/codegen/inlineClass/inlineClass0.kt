/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

// Based on KT-27225.

inline class First(val value: Int)

inline class Second(val value: First) {
    constructor(c: Int) : this(First(c))
}

fun box(): String {
    assertEquals(Second(42).value.value, 42)
    return "OK"
}

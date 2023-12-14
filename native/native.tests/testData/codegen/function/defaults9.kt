/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class Foo {
    inner class Bar(x: Int, val y: Int = 1) {
        constructor() : this(42)
    }
}

fun box(): String {
    assertEquals(1, Foo().Bar().y)
    return "OK"
}
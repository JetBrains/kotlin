/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    val x = Bar(42).x
    if (x != 42 ) return "FAIL: $x"
        return "OK"
}

class Foo(val x: Int)
typealias Bar = Foo
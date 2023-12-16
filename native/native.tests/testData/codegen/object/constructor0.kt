/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class A {
    var field0:Int = 0;
    constructor(arg0:Int) {
        field0 = arg0
    }
}

fun box(): String {
    assertEquals(42, A(42).field0)
    return "OK"
}

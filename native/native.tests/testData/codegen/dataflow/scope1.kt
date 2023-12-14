/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

var b = true

fun box(): String {
    var x = 1
    if (b) {
        var x = 2
    }
    assertEquals(1, x)
    return "OK"
}

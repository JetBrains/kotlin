/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

var log = ""

inline fun foo(x: Int, action: (Int) -> Unit) = action(x)

fun box(): String {
    var x = 23
    foo(x) {
        log += "$it;"
        x++
        log += "$it;"
    }

    if (log != "23;23;") return "fail1: $log"
    if (x != 24) return "fail2: $x"

    return "OK"
}

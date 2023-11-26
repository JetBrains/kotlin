/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

inline fun foo(x: Any) {
    println(if (x === x) "OK" else "Fail")
}

fun box(): String {
    foo { 42 }

    return "OK"
}

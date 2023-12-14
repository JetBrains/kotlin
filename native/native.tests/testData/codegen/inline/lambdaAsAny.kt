/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

inline fun foo(x: Any) {
    sb.append(if (x === x) "OK" else "FAIL")
}

fun box(): String {
    foo { 42 }

    return sb.toString()
}
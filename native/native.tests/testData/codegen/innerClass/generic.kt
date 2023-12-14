/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class Outer {
    inner class Inner<T>(val t: T) {
        fun box() = t
    }
}

fun box(): String {
    if (Outer().Inner("OK").box() != "OK") return "Fail"
    val x: Outer.Inner<String> = Outer().Inner("OK")
    return x.box()
}

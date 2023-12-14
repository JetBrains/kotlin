/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun foo() {
    val cls1: Any? = Int
    val cls2: Any? = null

    cls1?.let {
        if (cls2 != null) {
            val zzz = 42
        }
    }
}

fun box(): String {
    foo()
    return "OK"
}

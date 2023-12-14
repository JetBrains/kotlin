/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun foo() {
    val cls1: Any? = Int
    val cls2: Any? = null

    cls1?.let {
        cls2?.let {
            var itClass = it::class
        }
    }
}

fun box(): String {
    foo()
    return "OK"
}

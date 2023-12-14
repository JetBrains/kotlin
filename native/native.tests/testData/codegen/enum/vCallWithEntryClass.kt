/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

enum class Zzz {
    Z1 {
        override fun f() = "z1"
    },

    Z2 {
        override fun f() = "z2"
    };

    open fun f() = ""
}

fun box(): String {
    assertEquals("z1z2", Zzz.Z1.f() + Zzz.Z2.f())

    return "OK"
}
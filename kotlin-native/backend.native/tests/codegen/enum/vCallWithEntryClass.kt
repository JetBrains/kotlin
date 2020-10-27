/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.enum.vCallWithEntryClass

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

@Test fun runTest() {
    println(Zzz.Z1.f() + Zzz.Z2.f())
}
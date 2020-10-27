/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.enum.interfaceCallWithEntryClass

import kotlin.test.*

interface A {
    fun f(): String
}

enum class Zzz: A {
    Z1 {
        override fun f() = "z1"
    },

    Z2 {
        override fun f() = "z2"
    };

    override fun f() = ""
}

@Test fun runTest() {
    println(Zzz.Z1.f() + Zzz.Z2.f())
    val a1: A = Zzz.Z1
    val a2: A = Zzz.Z2
    println(a1.f() + a2.f())
}
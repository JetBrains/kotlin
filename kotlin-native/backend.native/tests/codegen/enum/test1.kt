/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.enum.test1

import kotlin.test.*

enum class Zzz(val zzz: String, val x: Int) {
    Z1("z1", 1),
    Z2("z2", 2)
}

@Test fun runTest() {
    println(Zzz.Z1.zzz + Zzz.Z2.x)
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.basics.superGetterCall

import kotlin.test.*

open class C {
    open val p1 = "<prop:C>"
}

class C1: C() {
    override val p1 = super<C>.p1 + "<prop:C1>"
}

open class C2: C() {
}

class C3: C2() {
    override val p1 = super<C2>.p1 + "<prop:C3>"
}

@Test
fun runTest() {
    println(C1().p1)
    println(C3().p1)
}
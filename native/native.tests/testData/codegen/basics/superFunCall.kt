/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

open class C {
    open fun f() = "<fun:C>"
}

class C1: C() {
    override fun f() = super<C>.f() + "<fun:C1>"
}

open class C2: C() {
}

class C3: C2() {
    override fun f() = super<C2>.f() + "<fun:C3>"
}

fun box(): String {
    val c1f = C1().f()
    if (c1f != "<fun:C><fun:C1>") return "FAIL 1: $c1f"
    val c3f = C3().f()
    if (c3f != "<fun:C><fun:C3>") return "FAIL 2: $c3f"

    return "OK"
}
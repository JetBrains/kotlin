/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

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

fun box(): String {
    val c1p1 = C1().p1
    if (c1p1 != "<prop:C><prop:C1>") return "FAIL 1: $c1p1"
    val c3p1 = C3().p1
    if (c3p1 != "<prop:C><prop:C3>") return "FAIl 2: $c3p1"

    return "OK"
}
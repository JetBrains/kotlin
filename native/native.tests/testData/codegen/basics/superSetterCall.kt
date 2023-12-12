/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

open class C {
    open var p2 = "<prop:C>"
        set(value)  { field = "<prop:C>" + value }
}

class C1: C() {
    override var p2 = super<C>.p2 + "<prop:C1>"
        set(value) {
            super<C>.p2 = value
            field = "<prop:C1>" + super<C>.p2
        }
}

open class C2: C() {
}

class C3: C2() {
    override var p2 = super<C2>.p2 + "<prop:C3>"
        set(value) {
            super<C2>.p2 = value
            field = "<prop:C3>" + super<C2>.p2
        }
}

fun box(): String {
    val c1 = C1()
    val c3 = C3()
    c1.p2 = "zzz"
    c3.p2 = "zzz"
    val c1p2 = c1.p2
    if (c1p2 != "<prop:C1><prop:C>zzz") return "FAIL 1: "
    val c3p2 = c3.p2
    if (c3p2 != "<prop:C3><prop:C>zzz") return "FAIL 2: "

    return "OK"
}
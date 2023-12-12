/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

// generic interface, non-generic impl, vtable call + interface call
open class A {
    open var size: Int = 56
}

interface C<T> {
    var size: T
}

open class B : C<Int>, A()

fun box(): String {
    val b = B()
    if (b.size != 56) return "fail 1"

    b.size = 55
    if (b.size != 55) return "fail 2"

    val c: C<Int> = b
    if (c.size != 55) return "fail 3"

    c.size = 57
    if (c.size != 57) return "fail 4"

    return "OK"
}

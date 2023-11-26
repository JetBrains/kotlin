/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class B(val a:Int, b:Int) {
    constructor(pos:Int):this(1, pos) {}
    val pos = b + 1
}

fun primaryConstructorCall(a:Int, b:Int) = B(a, b).pos

fun secondaryConstructorCall(a:Int) = B(a).pos

fun box(): String {
    if (primaryConstructorCall(0xdeadbeef.toInt(), 41) != 42) return "FAIL 1"
    if (secondaryConstructorCall(41)                   != 42) return "FAIL 2"

    return "OK"
}

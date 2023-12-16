/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

private var globalValue = 1
var global:Int
    get() = globalValue
    set(value:Int) {globalValue = value}

fun globalTest(i:Int):Int {
    global += i
    return global
}


fun box(): String {
    assertEquals(1, global)
    assertEquals(42, globalTest(41))
    assertEquals(42, global)

    return "OK"
}
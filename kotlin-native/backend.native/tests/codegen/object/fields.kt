/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`object`.fields

import kotlin.test.*

private var globalValue = 1
var global:Int
    get() = globalValue
    set(value:Int) {globalValue = value}

fun globalTest(i:Int):Int {
    global += i
    return global
}


@Test fun runTest() {
    if (global != 1)          throw Error()
    if (globalTest(41) != 42) throw Error()
    if (global != 42)         throw Error()
}
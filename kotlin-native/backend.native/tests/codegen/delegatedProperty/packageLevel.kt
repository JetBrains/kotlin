/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.delegatedProperty.packageLevel

import kotlin.test.*

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(receiver: Any?, p: KProperty<*>): Int {
        println(p.name)
        return 42
    }
}

val x: Int by Delegate()

@Test fun runTest() {
    println(x)
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.propertyCallableReference.dynamicReceiver

import kotlin.test.*

class TestClass {
    var x: Int = 42
}

fun foo(): TestClass {
    println(42)
    return TestClass()
}

@Test fun runTest() {
    foo()::x
}
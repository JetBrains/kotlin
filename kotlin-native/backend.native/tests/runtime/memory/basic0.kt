/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.memory.basic0

import kotlin.test.*

class A {
    var field: B? = null
}

class B(var field: Int)

@Test fun runTest() {
    val a = A()
    a.field = B(2)
}

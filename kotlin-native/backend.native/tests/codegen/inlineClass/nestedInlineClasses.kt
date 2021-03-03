/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inlineClass.nestedInlineClasses

import kotlin.test.*

interface I {
    inline class IC(val x: Int)
}

interface I2 {
    inline class IC(val x: Int)
}

@Test fun runTest() {
    assertEquals(42, I.IC(42).x)
    assertEquals(117, I2.IC(117).x)
}

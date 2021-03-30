/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.initializers4

import kotlin.test.*

const val INT_MAX_POWER_OF_TWO: Int = Int.MAX_VALUE / 2 + 1
val DOUBLE = Double.MAX_VALUE - 1.0

@Test fun runTest() {
    println(INT_MAX_POWER_OF_TWO)
    println(DOUBLE > 0.0)
}

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package test.numbers

import kotlin.math.*
import kotlin.test.*

class MathExceptionTest {

    @Test fun exceptions() {
        assertFails { Double.NaN.roundToLong() }
        assertFails { Double.NaN.roundToInt() }

        assertFails { Float.NaN.roundToLong() }
        assertFails { Float.NaN.roundToInt() }
    }

}
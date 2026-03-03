/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.misc

import samples.*

class BigIntegers {

    @Sample
    fun uintToBigInteger() {
        assertPrints(42u.toBigInteger(), "42")
        assertPrints(UInt.MAX_VALUE.toBigInteger(), "4294967295")
    }

    @Sample
    fun ulongToBigInteger() {
        assertPrints(42uL.toBigInteger(), "42")
        assertPrints(ULong.MAX_VALUE.toBigInteger(), "18446744073709551615")
    }
}

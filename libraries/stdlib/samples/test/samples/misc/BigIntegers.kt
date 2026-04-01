/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.misc

import samples.*

class BigIntegers {

    @Sample
    fun intToBigInteger() {
        assertPrints(42.toBigInteger(), "42")
        assertPrints(Int.MAX_VALUE.toBigInteger(), "2147483647")
        assertPrints(Int.MIN_VALUE.toBigInteger(), "-2147483648")
        // BigInteger can hold values beyond Int range
        assertPrints(Int.MAX_VALUE.toBigInteger() * 10.toBigInteger(), "21474836470")
    }

    @Sample
    fun longToBigInteger() {
        assertPrints(42L.toBigInteger(), "42")
        assertPrints(Long.MAX_VALUE.toBigInteger(), "9223372036854775807")
        assertPrints(Long.MIN_VALUE.toBigInteger(), "-9223372036854775808")
        // BigInteger can hold values beyond Long range
        assertPrints(Long.MAX_VALUE.toBigInteger() * 10.toBigInteger(), "92233720368547758070")
    }

    @Sample
    fun uintToBigInteger() {
        assertPrints(42u.toBigInteger(), "42")
        assertPrints(UInt.MAX_VALUE.toBigInteger(), "4294967295")
        // BigInteger can hold values beyond UInt range
        assertPrints(UInt.MAX_VALUE.toBigInteger() * 10.toBigInteger(), "42949672950")
    }

    @Sample
    fun ulongToBigInteger() {
        assertPrints(42uL.toBigInteger(), "42")
        assertPrints(ULong.MAX_VALUE.toBigInteger(), "18446744073709551615")
        // BigInteger can hold values beyond ULong range
        assertPrints(ULong.MAX_VALUE.toBigInteger() * 10.toBigInteger(), "184467440737095516150")
    }
}

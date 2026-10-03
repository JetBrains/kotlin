/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.misc

import samples.*
import java.math.BigDecimal

class BigDecimals {

    @Sample
    fun minus() {
        assertPrints(BigDecimal("100.5").minus(BigDecimal("42.2")), "58.3")
        assertPrints(BigDecimal("100.5") - BigDecimal("42.2"), "58.3")
        assertPrints(100.5.toBigDecimal().minus(42.2.toBigDecimal()), "58.3")
    }

    @Sample
    fun plus() {
        assertPrints(BigDecimal("100.5").plus(BigDecimal("42.2")), "142.7")
        assertPrints(BigDecimal("100.5") + BigDecimal("42.2"), "142.7")
        assertPrints(100.5.toBigDecimal().plus(42.2.toBigDecimal()), "142.7")
    }
}

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
        assertPrints(100.5.toBigDecimal() - 42.2.toBigDecimal(), "58.3")
    }

    @Sample
    fun plus() {
        assertPrints(100.5.toBigDecimal() + 42.2.toBigDecimal(), "142.7")
    }
}

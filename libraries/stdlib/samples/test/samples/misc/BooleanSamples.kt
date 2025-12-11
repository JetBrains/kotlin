/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.misc

import samples.*
import kotlin.test.assertTrue

class BooleanSamples {
    @Sample
    fun compareTo() {
        // false is equal to false, and true is equal to true
        assertPrints(false.compareTo(false), "0")
        assertPrints(true.compareTo(true), "0")

        // But false is less than true, and true is greater than false
        assertPrints(false.compareTo(true) < 0, "true")
        assertPrints(true.compareTo(false) > 0, "true")
    }
}

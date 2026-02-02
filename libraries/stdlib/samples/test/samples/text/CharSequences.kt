/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.text

import samples.*
import kotlin.test.*

class CharSequences {
    @Sample
    fun charSequenceLength() {
        assertPrints("".length, "0")
        assertPrints("Kotlin".length, "6")
        // 🥦 is represented by a pair of UTF-16 characters, thus the string's length is 2, not 1
        assertPrints("🥦".length, "2")
    }
}

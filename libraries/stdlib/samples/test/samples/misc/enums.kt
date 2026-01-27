/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.misc

import samples.*
import kotlin.test.*

class Enums {
    enum class Direction { NORTH, SOUTH, EAST, WEST }

    @Sample
    fun enumValueOfSample() {
        // enum class Direction { NORTH, SOUTH, EAST, WEST }

        val east = enumValueOf<Direction>("EAST")
        assertPrints(east, "EAST")

        // The lookup fails if a string does not correspond to any of the enum entries
        assertFailsWith<IllegalArgumentException> { enumValueOf<Direction>("UP") }
        assertFailsWith<IllegalArgumentException> { enumValueOf<Direction>("") }

        // The lookup is case-sensitive and fails if the name doesn't match any constant
        assertFailsWith<IllegalArgumentException> { enumValueOf<Direction>("east") }
    }
}

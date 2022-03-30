/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ExtrasIdStableStringTest {
    @Test
    fun `test - sample 0`() {
        assertEquals("kotlin.String", extrasIdOf<String>().stableString)
    }

    @Test
    fun `test - sample 1`() {
        assertEquals("kotlin.String;withName", extrasIdOf<String>("withName").stableString)
    }

    @Test
    fun `test - sample 2`() {
        assertEquals(extrasIdOf<String>(), Extras.Id.fromString(extrasIdOf<String>().stableString))
    }

    @Test
    fun `test - sample 3`() {
        assertEquals(extrasIdOf<String>("withName"), Extras.Id.fromString(extrasIdOf<String>("withName").stableString))
    }

    @Test
    fun `test - sample 4`() {
        assertEquals(
            extrasIdOf<List<Pair<Int, String>>>("withName"),
            Extras.Id.fromString(
                extrasIdOf<List<Pair<Int, String>>>("withName").stableString
            )
        )
    }
}

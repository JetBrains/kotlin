/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ExtrasKeyStableStringTest {
    @Test
    fun `test - sample 0`() {
        assertEquals("kotlin.String", extrasKeyOf<String>().stableString)
    }

    @Test
    fun `test - sample 1`() {
        assertEquals("kotlin.String;withName", extrasKeyOf<String>("withName").stableString)
    }

    @Test
    fun `test - sample 2`() {
        assertEquals(extrasKeyOf<String>(), Extras.Key.fromString(extrasKeyOf<String>().stableString))
    }

    @Test
    fun `test - sample 3`() {
        assertEquals(extrasKeyOf<String>("withName"), Extras.Key.fromString(extrasKeyOf<String>("withName").stableString))
    }

    @Test
    fun `test - sample 4`() {
        assertEquals(
            extrasKeyOf<List<Pair<Int, String>>>("withName"),
            Extras.Key.fromString(
                extrasKeyOf<List<Pair<Int, String>>>("withName").stableString
            )
        )
    }
}

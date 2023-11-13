/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

import org.junit.jupiter.api.*
import kotlin.test.*

class SirTest {
    // TODO: Just a fake test to validate that everything is working. Feel free to delete.
    @Test
    fun exampleTest() {
        val fakeElement = produceSwiftElement()
        assertTrue(fakeElement is SirElement)
    }

    private fun produceSwiftElement(): Any {
        return object : SirElement {}
    }
}
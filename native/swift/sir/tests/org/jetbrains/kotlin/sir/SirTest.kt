/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.sir.builder.buildEnum
import kotlin.test.Test
import kotlin.test.assertTrue

class SirTest {

    // TODO: Just a fake test to validate that everything is working. Feel free to delete.
    @Test
    fun exampleTest() {
        val fakeElement = produceSwiftElement()

        println(fakeElement)
        assertTrue(fakeElement is SirElement)
    }

    private fun produceSwiftElement(): Any {
        return buildEnum {
            origin = SirOrigin(path = listOf())
            name = "name"
            visibility = SirVisibility.PUBLIC
        }
    }
}
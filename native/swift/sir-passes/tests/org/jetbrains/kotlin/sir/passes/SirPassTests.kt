/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.passes

import org.jetbrains.kotlin.sir.SIR
import org.jetbrains.sir.passes.SirPass
import org.junit.Test
import kotlin.test.assertEquals

class SirPassTests {
    @Test
    fun smoke() {
        val elementDescription = "mySirElement"
        val mySirElement = object : SIR {
            override fun toString(): String {
                return elementDescription
            }
        }
        val myPass = object : SirPass<String, Unit> {
            override fun run(element: SIR, data: Unit): String {
                return element.toString()
            }
        }
        val result = myPass.run(mySirElement, Unit)
        assertEquals(elementDescription, result)
    }
}
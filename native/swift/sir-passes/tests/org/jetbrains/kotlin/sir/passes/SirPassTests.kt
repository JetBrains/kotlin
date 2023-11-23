/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.passes

import org.jetbrains.kotlin.sir.SirElement
import org.jetbrains.kotlin.sir.SirForeignFunction
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.buildForeignFunction
import org.jetbrains.sir.passes.SirPass
import kotlin.test.Test
import kotlin.test.assertEquals

class SirPassTests {
    @Test
    fun smoke() {
        val elementDescription = "mySirElement"
        val mySirElement = buildForeignFunction {
            origin = SirOrigin(listOf(elementDescription))
            visibility = SirVisibility.PUBLIC
        }
        val myPass = object : SirPass<List<String>?, Unit> {
            override fun run(element: SirElement, data: Unit): List<String>? {
                if (element is SirForeignFunction) {
                    return element.origin.path
                }
                return null
            }
        }
        val result = myPass.run(mySirElement, Unit)
        assertEquals(elementDescription, result?.first())
    }
}
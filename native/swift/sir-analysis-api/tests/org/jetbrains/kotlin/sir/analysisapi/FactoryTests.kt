/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.analysisapi

import org.jetbrains.kotlin.sir.SirElement
import org.junit.Test
import kotlin.test.assertEquals

class FactoryTests {
    @Test
    fun smoke() {
        val expectedString = "Element from factory"
        val myFactory = object : SirFactory {
            override fun build(): SirElement {
                return object : SirElement {
                    override fun toString(): String {
                        return expectedString
                    }
                }
            }
        }
        val element = myFactory.build()
        assertEquals(expectedString, element.toString())
    }
}
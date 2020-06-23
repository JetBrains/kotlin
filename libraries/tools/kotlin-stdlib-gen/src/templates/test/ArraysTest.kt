/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates.test

import templates.*
import templates.Family.*

object ArraysTest : TestTemplateGroupBase() {

    val f_copyInto = test("copyInto()") {
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
        exclude(PrimitiveType.Boolean)
    } builder {
        body {
            """
            val dest = ${of(1, 2, 3)}
            val newValues = ${of(4, 5, 6)}
            
            newValues.copyInto(dest, 0, 1, 3)
            val result1 = ${of(5, 6, 3)}
            assertTrue(result1 contentEquals dest, "Copying from newValues: ${"$"}{result1.contentToString()}, ${"$"}{dest.contentToString()}")

            dest.copyInto(dest, 0, 1, 3)
            val result2 = ${of(6, 3, 3)}
            assertTrue(result2 contentEquals dest, "Overlapping backward copy: ${"$"}{result2.contentToString()}, ${"$"}{dest.contentToString()}")

            dest.copyInto(dest, 1, 0, 2)
            val result3 = ${of(6, 6, 3)}
            assertTrue(result3 contentEquals dest, "Overlapping forward copy: ${"$"}{result2.contentToString()}, ${"$"}{dest.contentToString()}")
            """
        }
        body(PrimitiveType.Char) {
            """
            val dest = $of('a', 'b', 'c')
            val newValues = $of('e', 'f', 'g')
            newValues.copyInto(dest, 0, 1, 3)
            
            val result1 = $of('f', 'g', 'c')
            assertTrue(result1 contentEquals dest, "Copying from newValues: ${"$"}{result1.contentToString()}, ${"$"}{dest.contentToString()}")

            dest.copyInto(dest, 0, 1, 3)
            val result2 = $of('g', 'c', 'c')
            assertTrue(result2 contentEquals dest, "Overlapping backward copy: ${"$"}{result2.contentToString()}, ${"$"}{dest.contentToString()}")

            dest.copyInto(dest, 1, 0, 2)
            val result3 = $of('g', 'g', 'c')
            assertTrue(result3 contentEquals dest, "Overlapping forward copy: ${"$"}{result2.contentToString()}, ${"$"}{dest.contentToString()}") 
            """
        }
        body(ArraysOfObjects) {
            """
            val dest = arrayOf("a", "b", "c")
            val newValues = arrayOf("e", "f", "g")
            newValues.copyInto(dest, 0, 1, 3)
            
            val result1 = arrayOf("f", "g", "c")
            assertTrue(result1 contentEquals dest, "Copying from newValues: ${"$"}{result1.contentToString()}, ${"$"}{dest.contentToString()}")

            dest.copyInto(dest, 0, 1, 3)
            val result2 = arrayOf("g", "c", "c")
            assertTrue(result2 contentEquals dest, "Overlapping backward copy: ${"$"}{result2.contentToString()}, ${"$"}{dest.contentToString()}")

            dest.copyInto(dest, 1, 0, 2)
            val result3 = arrayOf("g", "g", "c")
            assertTrue(result3 contentEquals dest, "Overlapping forward copy: ${"$"}{result2.contentToString()}, ${"$"}{dest.contentToString()}")
            """
        }
        bodyAppend {
            """
            for ((start, end) in listOf(-1 to 0, 0 to 4, 4 to 4, 1 to 0, 0 to -1)) {
                val bounds = "start: ${"$"}start, end: ${"$"}end"
                val ex = assertFails(bounds) { newValues.copyInto(dest, 0, start, end) }
                assertTrue(ex is IllegalArgumentException || ex is IndexOutOfBoundsException, "Unexpected exception type: ${"$"}ex")
            }
            for (destIndex in listOf(-1, 2, 4)) {
                assertFailsWith<IndexOutOfBoundsException>("index: ${"$"}destIndex") { newValues.copyInto(dest, destIndex, 0, 2) }
            } 
            """
        }
    }
}
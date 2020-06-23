/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates.test

import templates.*
import templates.Family.*

object ElementsTest : TestTemplateGroupBase() {

    val f_indexOf = test("indexOf()") {
        include(Lists, Sequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        specialFor(ArraysOfPrimitives) {
            if (primitive!!.isFloatingPoint()) annotation("""@Suppress("DEPRECATION")""")
        }
        body {
            """
            expect(-1) { ${of(1, 2, 3)}.indexOf(ZERO) }
            expect(0) { ${of(1, 2, 3)}.indexOf(ONE) }
            expect(1) { ${of(1, 2, 3)}.indexOf(TWO) }
            expect(2) { ${of(1, 2, 3)}.indexOf(THREE) } 
            """
        }
        bodyAppend(Iterables, Sequences, ArraysOfObjects, Lists) {
            """
            expect(-1) { $of("cat", "dog", "bird").indexOf("mouse") }
            expect(0) { $of("cat", "dog", "bird").indexOf("cat") }
            expect(1) { $of("cat", "dog", "bird").indexOf("dog") }
            expect(2) { $of("cat", "dog", "bird").indexOf("bird") }
            expect(0) { $of(null, "dog", null).indexOf(null as String?)}
            """
        }
        body(PrimitiveType.Char) {
            """
            expect(-1) { charArrayOf('a', 'b', 'c').indexOf('z') }
            expect(0) { charArrayOf('a', 'b', 'c').indexOf('a') }
            expect(1) { charArrayOf('a', 'b', 'c').indexOf('b') }
            expect(2) { charArrayOf('a', 'b', 'c').indexOf('c') } 
            """
        }
        body(PrimitiveType.Boolean) {
            """
            expect(0) { booleanArrayOf(true, false).indexOf(true) }
            expect(1) { booleanArrayOf(true, false).indexOf(false) }
            expect(-1) { booleanArrayOf(true).indexOf(false) } 
            """
        }
    }

    val f_indexOfFirst = test("indexOfFirst()") {
        include(Lists, Sequences, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        body {
            """
            expect(-1) { ${of(1, 2, 3)}.indexOfFirst { it == ${toP(0)} } }
            expect(0) { ${of(1, 2, 3)}.indexOfFirst { it % TWO == ONE } }
            expect(1) { ${of(1, 2, 3)}.indexOfFirst { it % TWO == ZERO } }
            expect(2) { ${of(1, 2, 3)}.indexOfFirst { it == ${toP(3)} } }
            """
        }
        bodyAppend(Iterables, Sequences, ArraysOfObjects, Lists) {
            """
            expect(-1) { $of("cat", "dog", "bird").indexOfFirst { it.contains("p") } }
            expect(0) { $of("cat", "dog", "bird").indexOfFirst { it.startsWith('c') } }
            expect(1) { $of("cat", "dog", "bird").indexOfFirst { it.startsWith('d') } }
            expect(2) { $of("cat", "dog", "bird").indexOfFirst { it.endsWith('d') } }
            """
        }
        body(PrimitiveType.Char) {
            """
            expect(-1) { charArrayOf('a', 'b', 'c').indexOfFirst { it == 'z' } }
            expect(0) { charArrayOf('a', 'b', 'c').indexOfFirst { it < 'c' } }
            expect(1) { charArrayOf('a', 'b', 'c').indexOfFirst { it > 'a' } }
            expect(2) { charArrayOf('a', 'b', 'c').indexOfFirst { it != 'a' && it != 'b' } } 
            """
        }
        body(PrimitiveType.Boolean) {
            """
            expect(0) { booleanArrayOf(true, false, false, true).indexOfFirst { it } }
            expect(1) { booleanArrayOf(true, false, false, true).indexOfFirst { !it } }
            expect(-1) { booleanArrayOf(true, true).indexOfFirst { !it } } 
            """
        }
    }
}
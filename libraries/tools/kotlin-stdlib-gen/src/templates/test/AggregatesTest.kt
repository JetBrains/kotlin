/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates.test

import templates.*
import templates.Family.*

object AggregatesTest : TestTemplateGroupBase() {

    val f_minByOrNull = test("minByOrNull()") {
        includeDefault()
        include(ArraysOfUnsigned)
    } builder {

        body {
            """
            assertEquals(null, ${of()}.minByOrNull { it })
            assertEquals(ONE, ${of(1)}.minByOrNull { it })
            assertEquals(TWO, ${of(3, 2)}.minByOrNull { it * it })
            assertEquals(THREE, ${of(3, 2)}.minByOrNull { "a" })
            assertEquals(TWO, ${of(3, 2)}.minByOrNull { it.toString() })
            """
        }
        bodyAppend(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives) {
            """
            assertEquals(THREE, ${of(2, 3)}.minByOrNull { -it })
            """
        }
        bodyAppend(Iterables, Sequences, ArraysOfObjects) {
            """
            assertEquals('a', $of('a', 'b').minByOrNull { "x${"$"}it" })
            assertEquals("b", $of("b", "abc").minByOrNull { it.length })
            """
        }

        bodyAppend(PrimitiveType.Long) {
            """
            assertEquals(2000000000000, longArrayOf(3000000000000, 2000000000000).minByOrNull { it + 1 })
            """
        }
        body(PrimitiveType.Boolean) {
            """
            assertEquals(false, booleanArrayOf(true, false).minByOrNull { it.toString() })
            assertEquals(true, booleanArrayOf(true, false).minByOrNull { it.toString().length })
            """
        }
        body(PrimitiveType.Char) {
            """
            assertEquals('a', charArrayOf('a', 'b').minByOrNull { "x${"$"}it" })
            assertEquals('b', charArrayOf('b', 'a').minByOrNull { "${"$"}it".length })
            """
        }
    }

    val f_maxByOrNull = test("maxByOrNull()") {
        includeDefault()
        include(ArraysOfUnsigned)
    } builder {

        body {
            """
            assertEquals(null, ${of()}.maxByOrNull { it })
            assertEquals(ONE, ${of(1)}.maxByOrNull { it })
            assertEquals(THREE, ${of(3, 2)}.maxByOrNull { it * it })
            assertEquals(THREE, ${of(3, 2)}.maxByOrNull { "a" })
            assertEquals(THREE, ${of(3, 2)}.maxByOrNull { it.toString() })
            """
        }
        bodyAppend(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives) {
            """
            assertEquals(TWO, ${of(2, 3)}.maxByOrNull { -it })
            """
        }
        bodyAppend(Iterables, Sequences, ArraysOfObjects) {
            """
            assertEquals('b', $of('a', 'b').maxByOrNull { "x${"$"}it" })
            assertEquals("abc", $of("b", "abc").maxByOrNull { it.length })
            """
        }

        bodyAppend(PrimitiveType.Long) {
            """
            assertEquals(3000000000000, longArrayOf(3000000000000, 2000000000000).maxByOrNull { it + 1 })
            """
        }
        body(PrimitiveType.Boolean) {
            """
            assertEquals(true, booleanArrayOf(true, false).maxByOrNull { it.toString() })
            assertEquals(false, booleanArrayOf(true, false).maxByOrNull { it.toString().length })
            """
        }
        body(PrimitiveType.Char) {
            """
            assertEquals('b', charArrayOf('a', 'b').maxByOrNull { "x${"$"}it" })
            assertEquals('b', charArrayOf('b', 'a').maxByOrNull { "${"$"}it".length })
            """
        }
    }

    val f_minWithOrNull = test("minWithOrNull()") {
        includeDefault()
        include(ArraysOfUnsigned)
        exclude(PrimitiveType.Boolean, PrimitiveType.Char)
    } builder {

        body {
            """
            assertEquals(null, ${of()}.minWithOrNull(naturalOrder()))
            assertEquals(ONE, ${of(1)}.minWithOrNull(naturalOrder()))
            assertEquals(${literal(4)}, ${of(2, 3, 4)}.minWithOrNull(compareBy { it % ${literal(4)} }))
            """
        }
        bodyAppend(Iterables, Sequences, ArraysOfObjects) {
            """
            assertEquals("a", $of("a", "B").minWithOrNull(STRING_CASE_INSENSITIVE_ORDER))
            """
        }
    }

    val f_maxWithOrNull = test("maxWithOrNull()") {
        includeDefault()
        include(ArraysOfUnsigned)
        exclude(PrimitiveType.Boolean, PrimitiveType.Char)
    } builder {

        body {
            """
            assertEquals(null, ${of()}.maxWithOrNull(naturalOrder()))
            assertEquals(ONE, ${of(1)}.maxWithOrNull(naturalOrder()))
            assertEquals(${literal(3)}, ${of(2, 3, 4)}.maxWithOrNull(compareBy { it % ${literal(4)} }))
            """
        }
        bodyAppend(Iterables, Sequences, ArraysOfObjects) {
            """
            assertEquals("B", $of("a", "B").maxWithOrNull(STRING_CASE_INSENSITIVE_ORDER))
            """
        }
    }

    val f_foldIndexed = test("foldIndexed()") {
        includeDefault()
        include(ArraysOfUnsigned)
        exclude(PrimitiveType.Boolean, PrimitiveType.Char)
    } builder {

        body {
            """
            expect(${literal(8)}) { ${of(1, 2, 3)}.foldIndexed(ZERO) { i, acc, e -> acc + i.to$P() * e } }
            expect(10) { ${of(1, 2, 3)}.foldIndexed(1) { i, acc, e -> acc + i + e.toInt() } }
            expect(${literal(15)}) { ${of(1, 2, 3)}.foldIndexed(ONE) { i, acc, e -> acc * (i.to$P() + e) } }
            expect(" 0-${toString(1)} 1-${toString(2)} 2-${toString(3)}") { ${of(1, 2, 3)}.foldIndexed("") { i, acc, e -> "${"$"}acc ${"$"}i-${"$"}e" } }
            
            expect(${literal(42)}) {
                val numbers = ${of(1, 2, 3, 4)}
                numbers.foldIndexed(ZERO) { index, a, b -> index.to$P() * (a + b) }
            }
    
            expect(ZERO) {
                val numbers = ${of()}
                numbers.foldIndexed(ZERO) { index, a, b -> index.to$P() * (a + b) }
            }
    
            expect("${toString(1)}${toString(1)}${toString(2)}${toString(3)}${toString(4)}") {
                val numbers = ${of(1, 2, 3, 4)}
                numbers.map { it.toString() }.foldIndexed("") { index, a, b -> if (index == 0) a + b + b else a + b }
            }
            """
        }
    }

    val f_foldRightIndexed = test("foldRightIndexed()") {
        include(Lists, ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
        exclude(PrimitiveType.Boolean, PrimitiveType.Char)
    } builder {

        body {
            """
            expect(${literal(8)}) { ${of(1, 2, 3)}.foldRightIndexed(ZERO) { i, e, acc -> acc + i.to$P() * e } }
            expect(10) { ${of(1, 2, 3)}.foldRightIndexed(1) { i, e, acc -> acc + i + e.toInt() } }
            expect(${literal(15)}) { ${of(1, 2, 3)}.foldRightIndexed(ONE) { i, e, acc -> acc * (i.to$P() + e) } }
            expect(" 2-${toString(3)} 1-${toString(2)} 0-${toString(1)}") { ${of(1, 2, 3)}.foldRightIndexed("") { i, e, acc -> "${"$"}acc ${"$"}i-${"$"}e" } }
            
            expect("${toString(1)}${toString(2)}${toString(3)}${toString(4)}3210") {
                val numbers = ${of(1, 2, 3, 4)}
                numbers.map { it.toString() }.foldRightIndexed("") { index, a, b -> a + b + index }
            }
            """
        }
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.tests

import kotlin.test.*
import kotlin.test.tests.testFailureMessage

@OptIn(ExperimentalKotlinTestApi::class)
class AssertContainsTest {

    @Test
    fun testAssertContainsIterable() {
        val list = listOf(1, 2, 3)

        assertContains(list, 2)

        testFailureMessage("Expected the collection to contain the element.\nCollection <$list>, element <null>.") {
            assertContains(list as List<Any?>, null)
        }
        testFailureMessage("Expected the collection to contain the element.\nCollection <$list>, element <5>.") {
            assertContains(list, 5)
        }
    }

    @Test
    fun testAssertContainsIterableLazy() {
        val list = listOf(1, 2, 3)
        assertContains(list, 2) { fail() }

        val msg = "A value is missing"
        testFailureMessagesAreTheSame(
            expected = { assertContains(list, 4, msg) },
            actual = { assertContains(list, 4) { msg } }
        )
    }

    @Test
    fun testAssertContainsSequence() {
        val sequence = generateSequence(1) { it + 1 }.take(3)

        assertContains(sequence, 2)

        testFailureMessage("Expected the sequence to contain the element.\nSequence <$sequence>, element <null>.") {
            assertContains(sequence as Sequence<Any?>, null)
        }
        testFailureMessage("Expected the sequence to contain the element.\nSequence <$sequence>, element <5>.") {
            assertContains(sequence, 5)
        }
    }

    @Test
    fun testAssertContainsSequenceLazy() {
        val sequence = generateSequence(1) { it + 1 }.take(3)
        assertContains(sequence, 2) { fail() }

        val msg = "A value is missing"
        testFailureMessagesAreTheSame(
            expected = { assertContains(sequence, 4, msg) },
            actual = { assertContains(sequence, 4) { msg } }
        )
    }

    @Test
    fun testAssertContainsArray() {
        val array = arrayOf("Kot", "lin", "test")

        assertContains(array, "test")

        testFailureMessage("Expected the array to contain the element.\nArray <${array.contentToString()}>, element <null>.") {
            @Suppress("UNCHECKED_CAST")
            assertContains<Any?>(array as Array<Any?>, null)
        }
        testFailureMessage("Expected the array to contain the element.\nArray <${array.contentToString()}>, element <5>.") {
            assertContains(array, "5")
        }
    }

    @Test
    fun testAssertContainsArrayLazy() {
        val array = arrayOf("Kot", "lin", "test")
        assertContains(array, "test") { fail() }

        val msg = "A value is missing"
        testFailureMessagesAreTheSame(
            expected = { assertContains(array, "parameterized", msg) },
            actual = { assertContains(array, "parameterized") { msg } }
        )
    }

    @Test
    fun testAssertContainsCharArray() {
        val array = charArrayOf('x', 'y', 'z')

        assertContains(array, 'z')

        testFailureMessage("Expected the array to contain the element.\nArray <${array.contentToString()}>, element <5>.") {
            assertContains(array, '5')
        }
    }

    @Test
    fun testAssertContainsCharArrayLazy() {
        val array = charArrayOf('x', 'y', 'z')
        assertContains(array, 'z') { fail() }

        val msg = "A value is missing"
        testFailureMessagesAreTheSame(
            expected = { assertContains(array, 'a', msg) },
            actual = { assertContains(array, 'a') { msg } }
        )
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testAssertContainsUnsignedArray() {
        val array = ulongArrayOf(0u, ULong.MAX_VALUE, 2u)

        assertContains(array, ULong.MAX_VALUE)

        testFailureMessage("Expected the array to contain the element.\nArray <${array.contentToString()}>, element <5>.") {
            assertContains(array, 5u)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testAssertContainsUnsignedArrayLazy() {
        val array = ulongArrayOf(0u, ULong.MAX_VALUE, 2u)
        assertContains(array, ULong.MAX_VALUE) { fail() }

        val msg = "A value is missing"
        testFailureMessagesAreTheSame(
            expected = { assertContains(array, 5u, msg) },
            actual = { assertContains(array, 5u) { msg } }
        )
    }

    @Test
    fun testAssertContainsIntRange() {
        val range = -5..5

        assertContains(range, 0)

        testFailureMessage("Expected the range <-5..5> to contain the value <10>.") {
            assertContains(range, 10)
        }
    }

    @Test
    fun testAssertContainsIntRangeLazy() {
        val range = -5..5
        assertContains(range, 0) { fail() }

        val msg = "A value is missing"
        testFailureMessagesAreTheSame(
            expected = { assertContains(range, 15, msg) },
            actual = { assertContains(range, 15) { msg } }
        )
    }

    @Test
    fun testAssertContainsCharRange() {
        val range = 'a'..'y'

        assertContains(range, 'f')

        testFailureMessage("Expected the range <a..y> to contain the value <A>.") {
            assertContains(range, 'A')
        }
    }

    @Test
    fun testAssertContainsCharRangeLazy() {
        val range = 'a'..'k'
        assertContains(range, 'f') { fail() }

        val msg = "A value is missing"
        testFailureMessagesAreTheSame(
            expected = { assertContains(range, 'n', msg) },
            actual = { assertContains(range, 'n') { msg } }
        )
    }

    @Test
    fun testAssertContainsDoubleRange() {
        val range = 0.5..0.55

        assertContains(range, 0.52)

        val one = 1.0
        testFailureMessage("Expected the range <$range> to contain the value <$one>.") {
            assertContains(range, one)
        }
    }

    @Test
    fun testAssertContainsDoubleRangeLazy() {
        val range = 0.5..0.55
        assertContains(range, 0.52) { fail() }

        val msg = "A value is missing"
        testFailureMessagesAreTheSame(
            expected = { assertContains(range, 2.0, msg) },
            actual = { assertContains(range, 2.0) { msg } }
        )
    }

    @Test
    @OptIn(ExperimentalStdlibApi::class)
    fun assertContainsOpenRange() {
        val range = 0.0.rangeUntil(1.0) // TODO: replace with ..< by 1.8
        assertContains(range, 0.99)

        val one = 1.0
        testFailureMessage("Expected the range <$range> to contain the value <$one>.") {
            assertContains(range, one)
        }
    }

    @Test
    fun assertContainsOpenRangeLazy() {
        val range = 0.0..<1.8
        assertContains(range, 0.99) { fail() }

        val msg = "A value is missing"
        testFailureMessagesAreTheSame(
            expected = { assertContains(range, 2.0, msg) },
            actual = { assertContains(range, 2.0) { msg } }
        )
    }

    @Test
    fun testAssertContainsMap() {
        val map = mapOf(
            "apple" to "green",
            "banana" to "yellow",
            "orange" to "orange",
        )

        assertContains(map, "apple")

        testFailureMessage("Expected the map to contain the key.\nMap <$map>, key <pineapple>.") {
            assertContains(map, "pineapple")
        }
    }

    @Test
    fun testAssertContainsMapLazy() {
        val map = mapOf(
            "apple" to "green",
            "banana" to "yellow",
            "orange" to "orange",
        )
        assertContains(map, "apple") { fail() }

        val msg = "A value is missing"
        testFailureMessagesAreTheSame(
            expected = { assertContains(map, "guava", msg) },
            actual = { assertContains(map, "guava") { msg } }
        )
    }

    @Test
    fun testAssertContainsCharSequence() {
        val string = "Pineapple"

        assertContains(string, 'e')
        assertContains(string, 'N', ignoreCase = true)
        assertContains(string, "app")
        assertContains(string, "ApP", ignoreCase = true)
        assertContains(string, Regex("[a-zA-Z]"))

        testFailureMessage("Expected the char sequence to contain the char.\nCharSequence <$string>, char <N>, ignoreCase <false>.") {
            assertContains(string, 'N')
        }
        testFailureMessage("Expected the char sequence to contain the char.\nCharSequence <$string>, char <x>, ignoreCase <true>.") {
            assertContains(string, 'x', ignoreCase = true)
        }
        testFailureMessage("Expected the char sequence to contain the substring.\nCharSequence <$string>, substring <ApP>, ignoreCase <false>.") {
            assertContains(string, "ApP")
        }
        testFailureMessage("Expected the char sequence to contain the substring.\nCharSequence <$string>, substring <Appleseed>, ignoreCase <true>.") {
            assertContains(string, "Appleseed", ignoreCase = true)
        }

        val digit = Regex("[0-9]")
        testFailureMessage("Expected the char sequence to contain the regular expression.\nCharSequence <$string>, regex <$digit>.") {
            assertContains(string, digit)
        }
    }

    @Test
    fun testAssertContainsCharSequenceLazy() {
        val string = "Pineapple"

        val msg = "A value is missing"

        assertContains(string, 'e') { fail() }
        testFailureMessagesAreTheSame(
            expected = { assertContains(string, 'x', message = msg) },
            actual = { assertContains(string, 'x') { msg } }
        )

        assertContains(string, 'N', ignoreCase = true) { fail() }
        testFailureMessagesAreTheSame(
            expected = { assertContains(string, 'X', ignoreCase = true, msg) },
            actual = { assertContains(string, 'X', ignoreCase = true) { msg } }
        )

        assertContains(string, "app") { fail() }
        testFailureMessagesAreTheSame(
            expected = { assertContains(string, "guava", message = msg) },
            actual = { assertContains(string, "guava") { msg } }
        )

        assertContains(string, "ApP", ignoreCase = true) { fail() }
        testFailureMessagesAreTheSame(
            expected = { assertContains(string, "Guava", ignoreCase = true, msg) },
            actual = { assertContains(string, "Guava", ignoreCase = true) { msg } }
        )

        assertContains(string, Regex("[a-zA-Z]")) { fail() }
        testFailureMessagesAreTheSame(
            expected = { assertContains(string, Regex("[0-9]"), msg) },
            actual = { assertContains(string, Regex("[0-9]")) { msg } }
        )
    }
}

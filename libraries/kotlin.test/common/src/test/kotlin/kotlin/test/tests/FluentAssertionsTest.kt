/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.tests

import kotlin.test.*
import kotlin.test.tests.testFailureMessage

class FluentAssertionsTest {
    @Test
    fun testShouldBe() {
        1 shouldBe 1
    }

    @Test
    fun testShouldBeFailure() {
        testFailureMessage("Expected <2>, actual <1>.") {
            1 shouldBe 2
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun testShouldBeForDoubleNotAllowed() {
        assertFailsWith<UnsupportedOperationException> {
            0.01 shouldBe 0.01
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun testShouldNotBeForDoubleNotAllowed() {
        assertFailsWith<UnsupportedOperationException> {
            0.01 shouldNotBe 0.01
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun testShouldBeForFloatNotAllowed() {
        assertFailsWith<UnsupportedOperationException> {
            0.01 shouldBe 0.01
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun testShouldNotBeForFloatNotAllowed() {
        assertFailsWith<UnsupportedOperationException> {
            0.01 shouldNotBe 0.01
        }
    }

    @Test
    fun testShouldBeSameAs() {
        val instance: Any = object {}
        instance shouldBeSameAs instance
    }

    @Test
    fun testShouldBeSameAsFailure() {
        val one: Any = object {
            override fun toString() = "One"
        }
        val another: Any = object {
            override fun toString() = "Another"
        }
        testFailureMessage("Expected <Another>, actual <One> is not same.") {
            one shouldBeSameAs another
        }
    }

    @Test
    fun testShouldBeString() {
        "Friends" shouldBe "Friends"
    }

    @Test
    fun testShouldBeStringFailure() {
        testFailureMessage("Expected <Rivals>, actual <Friends>.") {
            "Friends" shouldBe "Rivals"
        }
    }

    @Test
    fun testShouldNotBeString() {
        "Friends" shouldNotBe "Rivals"
    }

    @Test
    fun testShouldNotBeStringFailure() {
        testFailureMessage("Illegal value: <Friends>.") {
            "Friends" shouldNotBe "Friends"
        }
    }

    @Test
    fun testShouldNotBe() {
        1 shouldNotBe 2
    }

    @Test
    fun testShouldNotBeFailure() {
        testFailureMessage("Illegal value: <1>.") {
            1 shouldNotBe 1
        }
    }

    @Test
    fun testShouldNotBeSameAs() {
        val one: Any = object {}
        val another: Any = object {}
        one shouldNotBeSameAs another
    }

    @Test
    fun testShouldNotBeSameAsFailure() {
        val instance: Any = object {
            override fun toString() = "Instance"
        }
        testFailureMessage("Expected not same as <Instance>.") {
            instance shouldNotBeSameAs instance
        }
    }

    @Test
    fun testShouldContainIterable() {
        val list = listOf(1, 2, 3)
        list shouldContain 2
    }

    @Test
    fun testShouldContainIterableFailure() {
        val list = listOf(1, 2, 3)
        testFailureMessage("Expected the collection to contain the element.\nCollection <$list>, element <4>.") {
            list shouldContain 4
        }
    }

    @Test
    fun testShouldContainSequence() {
        val sequence = sequenceOf(1, 2, 3)
        sequence shouldContain 2
    }

    @Test
    fun testShouldContainSequenceFailure() {
        val sequence = sequenceOf(1, 2, 3)
        testFailureMessage("Expected the sequence to contain the element.\nSequence <$sequence>, element <4>.") {
            sequence shouldContain 4
        }
    }

    @Test
    fun testShouldContainCharSequence() {
        val text = "Hello World"
        text shouldContain "World"
    }

    @Test
    fun testShouldContainCharSequenceFailure() {
        val text = "Hello World"
        testFailureMessage("Expected the char sequence to contain the substring.\nCharSequence <$text>, substring <Goodbye>, ignoreCase <false>.") {
            text shouldContain "Goodbye"
        }
    }

    @Test
    fun testWithClueString() {
        withClue("Test message") {
            1 shouldBe 1
        }
    }

    @Test
    fun testWithClueStringFailure() {
        testFailureMessage("Test message. Expected <2>, actual <1>.") {
            withClue("Test message") {
                1 shouldBe 2
            }
        }
    }

    @Test
    fun testWithClueStringNull() {
        testFailureMessage("Test message. Expected <2>, actual <1>.") {
            withClue("Test message") {
                withClue(null as String?) {
                    1 shouldBe 2
                }
            }
        }
    }

    @Test
    fun testWithClueLazy() {
        withClue({ "Lazy message" }) {
            1 shouldBe 1
        }
    }

    @Test
    fun testWithClueLazyFailure() {
        testFailureMessage("Lazy message. Expected <2>, actual <1>.") {
            withClue({ "Lazy message" }) {
                1 shouldBe 2
            }
        }
    }

    @Test
    fun testWithClueLazyNull() {
        withClue({ null }) {
            1 shouldBe 1
        }
    }

    @Test
    fun testWithClueLazyNullFailure() {
        testFailureMessage("Expected <2>, actual <1>.") {
            withClue({ null }) {
                1 shouldBe 2
            }
        }
    }

    @Test
    fun testWithClueNonAssertionError() {
        val error = testFailureMessage("Custom message. Original error") {
            withClue("Custom message") {
                throw IllegalStateException("Original error")
            }
        }
        assertTrue(error.cause is IllegalStateException)
    }

    @Test
    fun testShouldStartWithCharSequence() {
        "Hello World" shouldStartWith "Hello"
    }

    @Test
    fun testShouldStartWithCharSequenceFailure() {
        testFailureMessage("Expected <Hello World> to start with <Goodbye>.") {
            "Hello World" shouldStartWith "Goodbye"
        }
    }

    @Test
    fun testShouldEndWithCharSequence() {
        "Hello World" shouldEndWith "World"
    }

    @Test
    fun testShouldEndWithCharSequenceFailure() {
        testFailureMessage("Expected <Hello World> to end with <Goodbye>.") {
            "Hello World" shouldEndWith "Goodbye"
        }
    }

    @Test
    fun testShouldStartWithIterable() {
        listOf(1, 2, 3, 4).shouldStartWith(1, 2)
    }

    @Test
    fun testShouldStartWithElement() {
        listOf(1, 2, 3, 4).shouldStartWith(1)
    }

    @Test
    fun testShouldStartWithIterableFailure() {
        val list = listOf(1, 2, 3, 4)
        testFailureMessage("Expected <$list> to start with <[3, 4]>, but differs at index 0: expected <3>, actual <1>.") {
            list.shouldStartWith(3, 4)
        }
    }

    @Test
    fun testShouldStartWithElementFailure() {
        val list = listOf(1, 2, 3, 4)
        testFailureMessage("Expected <$list> to start with <3>.") {
            list shouldStartWith 3
        }
    }

    @Test
    fun testShouldStartWithIterableSizeFailure() {
        val list = listOf(1, 2)
        testFailureMessage("Expected <$list> to start with <[1, 2, 3]>, but actual size 2 is less than expected size 3.") {
            list.shouldStartWith(1, 2, 3)
        }
    }

    @Test
    fun testShouldEndWithIterable() {
        listOf(1, 2, 3, 4).shouldEndWith(3, 4)
    }

    @Test
    fun testShouldEndWithElement() {
        listOf(1, 2, 3, 4) shouldEndWith 4
    }

    @Test
    fun testShouldEndWithIterableFailure() {
        val list = listOf(1, 2, 3, 4)
        testFailureMessage("Expected <$list> to end with <[1, 2]>, but differs at index 2: expected <1>, actual <3>.") {
            list.shouldEndWith(1, 2)
        }
    }

    @Test
    fun testShouldEndWithIterableSizeFailure() {
        val list = listOf(1, 2)
        testFailureMessage("Expected <$list> to end with <[1, 2, 3]>, but actual size 2 is less than expected size 3.") {
            list.shouldEndWith(1, 2, 3)
        }
    }

    @Test
    fun testShouldEndWithElementFailure() {
        val list = listOf(1, 2, 3, 4)
        testFailureMessage("Expected <$list> to end with <1>.") {
            list shouldEndWith 1
        }
    }
}

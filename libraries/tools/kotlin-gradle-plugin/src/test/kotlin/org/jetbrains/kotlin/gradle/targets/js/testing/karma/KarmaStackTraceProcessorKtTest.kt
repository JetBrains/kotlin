/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

import org.junit.Assert.assertEquals
import org.junit.Test

class KarmaStackTraceProcessorKtTest {

    @Test
    fun processKarmaStackTrace() {
        val stackTrace = """AssertionError: Expected value to be true.
    at AssertionError_init_0 (webpack:///kotlin/exceptions.kt:102:36 <- /Users/ilya.goncharov/repos/check-kotlin-js-test/build/js/packages/check-kotlin-js-test-test/adapter.js:33390:22)
    at DefaultJsAsserter.failWithMessage_0 (webpack:///src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:80:18 <- /Users/ilya.goncharov/repos/check-kotlin-js-test/build/js/packages/check-kotlin-js-test-test/adapter.js:1569:13)
    at DefaultJsAsserter.assertTrue_o10pc4${'$'} (webpack:///src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:60:12 <- /Users/ilya.goncharov/repos/check-kotlin-js-test/build/js/packages/check-kotlin-js-test-test/adapter.js:1536:12)
    at DefaultJsAsserter.assertTrue_4mavae${'$'} (webpack:///src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:67:8 <- /Users/ilya.goncharov/repos/check-kotlin-js-test/build/js/packages/check-kotlin-js-test-test/adapter.js:1548:10)
    at assertTrue_0 (webpack:///Assertions.kt:37:20 <- /Users/ilya.goncharov/repos/check-kotlin-js-test/build/js/packages/check-kotlin-js-test-test/adapter.js:1182:27)
    at assertTrue (webpack:///Assertions.kt:32:70 <- /Users/ilya.goncharov/repos/check-kotlin-js-test/build/js/packages/check-kotlin-js-test-test/adapter.js:1177:5)
    at MyTest../kotlin/check-kotlin-js-test-test.js.MyTest.foo (webpack:///src/test/kotlin/MyTest.kt:7:8 <- /Users/ilya.goncharov/repos/check-kotlin-js-test/build/js/packages/check-kotlin-js-test-test/adapter.js:53047:5)
    at Context.<anonymous> (/Users/ilya.goncharov/repos/check-kotlin-js-test/build/js/packages/check-kotlin-js-test-test/adapter.js:53080:31)""".trimIndent()

        val expected = """AssertionError: Expected value to be true.
    at AssertionError_init_0 (/kotlin/exceptions.kt:102:36)
    at DefaultJsAsserter.failWithMessage_0 (/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:80:18)
    at DefaultJsAsserter.assertTrue_o10pc4${'$'} (/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:60:12)
    at DefaultJsAsserter.assertTrue_4mavae${'$'} (/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:67:8)
    at assertTrue_0 (/Assertions.kt:37:20)
    at assertTrue (/Assertions.kt:32:70)
    at MyTest.foo (/src/test/kotlin/MyTest.kt:7:8)
    at Context.<anonymous> (/Users/ilya.goncharov/repos/check-kotlin-js-test/build/js/packages/check-kotlin-js-test-test/adapter.js:53080:31)""".trimIndent()

        assertEquals(
            expected,
            processKarmaStackTrace(stackTrace)
        )
    }

    @Test
    fun processWebpackName() {
        val processedLine = processWebpackName(
            "at MyTest../kotlin/check-js-test-test.js.MyTest.foo (/src/test/kotlin/MyTest.kt:7:8)"
        )
        assertEquals(
            "at MyTest.foo (/src/test/kotlin/MyTest.kt:7:8)",
            processedLine
        )
    }

    @Test
    fun notProcessNotWebpackName() {
        val line = "at DefaultJsAsserter.assertTrue(/src/main/kotlin/kotlin/test/DefaultJsAsserter.kt:67)"
        assertEquals(
            line,
            processWebpackName(line)
        )
    }

    @Test
    fun notProcessMessage() {
        val line = "AssertionError: Expected value to be true."
        assertEquals(
            line,
            processWebpackName(line)
        )
    }
}
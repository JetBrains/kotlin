/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.util.GTestListing
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("infrastructure")
class InfrastructureGTestListingTest {
    @Test
    fun successfullyParsed() = assertEquals(
        listOf(
            "Foo.bar",
            "Foo.baz",
            "a.Foo.bar",
            "a.Foo.baz",
            "a.b.Foo.bar",
            "a.b.Foo.baz",
            "FooKt.bar",
            "FooKt.baz",
            "a.FooKt.bar",
            "a.FooKt.baz",
            "a.b.FooKt.bar",
            "a.b.FooKt.baz",
            "__launcher__Kt.bar",
            "__launcher__Kt.baz",
            "a.__launcher__Kt.bar",
            "a.__launcher__Kt.baz",
            "a.b.__launcher__Kt.bar",
            "a.b.__launcher__Kt.baz",
        ).map(::TestName),
        GTestListing.parse(
            """
                |Seed: 123
                |Seed: 456
                |Seed: -789
                |Foo.
                |  bar
                |  baz
                |a.Foo.
                |  bar
                |  baz
                |a.b.Foo.
                |  bar
                |  baz
                |FooKt.
                |  bar
                |  baz
                |a.FooKt.
                |  bar
                |  baz
                |a.b.FooKt.
                |  bar
                |  baz
                |__launcher__Kt.
                |  bar
                |  baz
                |a.__launcher__Kt.
                |  bar
                |  baz
                |a.b.__launcher__Kt.
                |  bar
                |  baz
                |
                |
                |
            """.trimMargin()
        )
    )

    @Test
    fun unexpectedEmptyLine1() = assertCorrectParseError(
        "Unexpected empty line",
        0,
        """
            |
            |foo.
            |  bar
        """.trimMargin()
    )

    @Test
    fun unexpectedEmptyLine2() = assertCorrectParseError(
        "Unexpected empty line",
        0,
        """
            |
            |  foo
            |bar.
            |  baz
        """.trimMargin()
    )

    @Test
    fun unexpectedEmptyLine3() = assertCorrectParseError(
        "Unexpected empty line",
        1,
        """
            |foo.
            |
            |  bar
        """.trimMargin()
    )

    @Test
    fun unexpectedEmptyLine4() = assertCorrectParseError(
        "Unexpected empty line",
        3,
        """
            |foo.
            |  bar
            |baz.
            |
        """.trimMargin()
    )

    @Test
    fun testNameBeforeTestSuiteName() = assertCorrectParseError(
        "Test name encountered before test suite name",
        0,
        """
            |  foo
            |bar.
            |  baz
        """.trimMargin()
    )

    @Test
    fun unexpectedTestSuiteName() = assertCorrectParseError(
        "Unexpected test suite name",
        1,
        """
            |foo.
            |bar.
            |  baz
        """.trimMargin()
    )

    @Test
    fun noTestNameAfterTestSuiteName() = assertCorrectParseError(
        "Test name expected before test suite name",
        2,
        """
            |foo.
            |  bar
            |baz.
        """.trimMargin()
    )

    companion object {
        private fun assertCorrectParseError(expectedMessage: String, lineNumber: Int, listing: String) {
            try {
                GTestListing.parse(listing)
                fail { "Listing parsed without errors" }
            } catch (e: AssertionError) {
                val message = e.message.orEmpty()
                if (message.startsWith(expectedMessage) && "at line #$lineNumber" in message) {
                    // it's OK
                } else
                    throw e
            }
        }
    }
}

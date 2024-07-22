/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.printErrorForXcode
import kotlin.test.Test
import kotlin.test.assertEquals

class XcodeMessageReportingTests {
    class ErrorStack(
        val message: String,
        val causes: List<ErrorStack> = emptyList()
    ) { constructor(message: String, vararg causes: ErrorStack) : this(message, causes.asList()) }

    fun List<ErrorStack>.dumpMessagesList(
        lineLimit: Int = 5,
        lineLimitMessage: String = "limit",
    ): List<String> {
        val messages = mutableListOf<String>()
        printErrorForXcode(
            causes = { it.causes },
            message = { it.message },
            printLine = {
                messages.add(it)
            },
            lineLimit = lineLimit,
            lineLimitMessage = lineLimitMessage,
        )
        return messages
    }

    @Test
    fun `single message`() {
        assertEquals(
            listOf("1"),
            listOf(
                ErrorStack("1")
            ).dumpMessagesList()
        )
    }

    @Test
    fun `deep stack of messages - prints correctly ordered and indented`() {
        assertEquals(
            listOf(
                "1",
                "  2",
                "    3",
                "    4",
                "  5",
                "    6",
                "    7",
                "8",
                "  9"
            ),
            listOf(
                ErrorStack(
                    "1",
                    ErrorStack(
                        "2",
                        ErrorStack("3"),
                        ErrorStack("4")
                    ),
                    ErrorStack(
                        "5",
                        ErrorStack("6"),
                        ErrorStack("7")
                    ),
                ),
                ErrorStack(
                    "8",
                    ErrorStack("9")
                )
            ).dumpMessagesList(lineLimit = 10)
        )
    }

    @Test
    fun `line limit - replaces last line - when the line limit is exceeded`() {
        assertEquals(
            listOf(
                "1",
                "  2",
                "    3",
                "limit",
            ),
            listOf(
                ErrorStack(
                    "1",
                    ErrorStack(
                        "2",
                        ErrorStack("3"),
                        ErrorStack("4")
                    )
                )
            ).dumpMessagesList(
                lineLimit = 4,
                lineLimitMessage = "limit"
            )
        )
    }

    @Test
    fun `line limit - works per line in the message and messages with newlines are correctly indented`() {
        assertEquals(
            listOf(
                "1",
                "2",
                "  3",
                "  4",
                "    5",
                "limit",
            ),
            listOf(
                ErrorStack(
                    "1\n2",
                    ErrorStack(
                        "3\n4",
                        ErrorStack("5\n6"),
                    )
                )
            ).dumpMessagesList(
                lineLimit = 6,
                lineLimitMessage = "limit"
            )
        )
    }

}
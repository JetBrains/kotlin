/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JUnitTestCaseWithNoTests")

package org.jetbrains.kotlin.gradle.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class StringBlockBuilderTest {

    @Test
    fun `test buildStringBlock with default indent`() {
        assertEquals(
            """
            |Line 1
            |{
            |    Line 2
            |    {
            |        Nested Line
            |    }
            |    Line 3
            |    c1\
            |        c2\
            |        c3
            |    Line 4
            |}
            |""".trimMargin(),
            buildStringBlock {
                line("Line 1")
                block("{", "}") {
                    line("Line 2")
                    block("{", "}") {
                        line("Nested Line")
                    }
                    line("Line 3")
                    connectedLines("\\") {
                        line("c1")
                        line("c2")
                        line("c3")
                    }
                    line("Line 4")
                }
            }
        )
    }

    @Test
    fun `test buildStringBlock with custom indent`() {
        assertEquals(
            """
            |Line 1
            |{
            | > Line 2
            | > {
            | >  > Nested Line
            | > }
            | > Line 3
            | > c1\
            | >  > c2\
            | >  > c3
            | > Line 4
            |}
            |""".trimMargin(),
            buildStringBlock(defaultIndent = " > ") {
                line("Line 1")
                block("{", "}") {
                    line("Line 2")
                    block("{", "}") {
                        line("Nested Line")
                    }
                    line("Line 3")
                    connectedLines("\\") {
                        line("c1")
                        line("c2")
                        line("c3")
                    }
                    line("Line 4")
                }
            }
        )
    }

    @Test
    fun `test buildStringBlock with empty block`() {
        assertEquals(
            """
            |OPEN
            |CLOSE
            |""".trimMargin(),
            buildStringBlock {
                block("OPEN", "CLOSE") {
                }
            }
        )
    }

    @Test
    fun `test buildStringBlock with empty block, surrounded with lines`() {
        assertEquals(
            """
            |line1
            |OPEN
            |CLOSE
            |line2
            |""".trimMargin(),
            buildStringBlock {
                line("line1")
                block("OPEN", "CLOSE") {
                }
                line("line2")
            }
        )
    }

    @Test
    fun `test buildStringBlock with empty block, no open or close`() {
        assertEquals(
            "",
            buildStringBlock {
                block("", "") {}
            }
        )
    }

    @Test
    fun `test buildStringBlock with empty block, no open or close, surrounded with lines`() {
        assertEquals(
            """
            |line1
            |line2
            |""".trimMargin(),
            buildStringBlock {
                line("line1")
                block("", "") {}
                line("line2")
            }
        )
    }

    @Test
    fun `test connectedLines with single line`() {
        assertEquals(
            "singleLine\n",
            buildStringBlock {
                connectedLines("\\") {
                    line("singleLine")
                }
            }
        )
    }

    @Test
    fun `test connectedLines with multiple lines`() {
        assertEquals(
            """
            |line1\
            |    line2\
            |    line3\
            |    line4\
            |    line5
            |""".trimMargin(),
            buildStringBlock {
                connectedLines("\\") {
                    line("line1")
                    line("line2")
                    line("line3")
                    line("line4")
                    line("line5")
                }
            }
        )
    }

    @Test
    fun `test connectedLines with no separator`() {
        assertEquals(
            """
           |line1
           |    line2
           |    line3
           |""".trimMargin(),
            buildStringBlock {
                connectedLines("") {
                    line("line1")
                    line("line2")
                    line("line3")
                }
            }
        )
    }

    @Test
    fun `test connectedLines with empty block`() {
        assertEquals(
            "",
            buildStringBlock {
                connectedLines("\\") {}
            }
        )
    }

    @Test
    fun `test connectedLines with empty block, surrounded with lines`() {
        assertEquals(
            """
            |line1
            |line2
            |""".trimMargin(),
            buildStringBlock {
                line("line1")
                connectedLines("\\") {}
                line("line2")
            }
        )
    }

    @Test
    fun `test connectedLines with blank lines`() {
        assertEquals(
            """
            |<sep>
            |    <sep>
            |""".trimMargin(),
            buildStringBlock {
                connectedLines("<sep>") {
                    line("")
                    line("")
                    line("")
                }
            }
        )
    }

    @Test
    fun `test connectedLines with blank lines, surrounded with lines`() {
        assertEquals(
            """
            |line1
            |\
            |    \
            |
            |line2
            |""".trimMargin(),
            buildStringBlock {
                line("line1")
                connectedLines("\\") {
                    line("")
                    line("")
                    line("")
                }
                line("line2")
            }
        )
    }

    @Test
    fun `test connectedLines with blank line, surrounded with lines`() {
        assertEquals(
            """
            |line1
            |
            |line2
            |""".trimMargin(),
            buildStringBlock {
                line("line1")
                connectedLines("\\") {
                    line("")
                }
                line("line2")
            }
        )
    }

    @Test
    fun `test connectedLines containing multiline strings`() {
        assertEquals(
            """
            |start---
            |    a
            |    multiline
            |    string---
            |    end
            |""".trimMargin(),
            buildStringBlock {
                connectedLines("---") {
                    line("start")
                    line("a\nmultiline\nstring")
                    line("end")
                }
            }
        )
    }

    @Test
    fun `test combination of empty connectedLines and block indentation`() {
        assertEquals(
            """
            |Start
            |{
            |    line1
            |    line2
            |}
            |End
            |""".trimMargin(),
            buildStringBlock {
                line("Start")
                block("{", "}") {
                    line("line1")
                    line("line2")
                    connectedLines("\\") {}
                }
                line("End")
            }
        )
    }

    @Test
    fun `test buildStringBlock with combination of connectedLines and nested blocks`() {
        assertEquals(
            """
            |Start Line
            |{
            |    conLine1 \
            |        conLine2 \
            |        conLine3
            |}
            |Final Line
            |""".trimMargin(),
            buildStringBlock {
                line("Start Line")
                block("{", "}") {
                    connectedLines(" \\") {
                        line("conLine1")
                        line("conLine2")
                        line("conLine3")
                    }
                }
                line("Final Line")
            }
        )
    }
}

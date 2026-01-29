/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JUnitTestCaseWithNoTests")

package org.jetbrains.kotlin.gradle.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class StringBlockBuilderTest {

    // --- emitListItems ---

    // Single item: no comma added (nothing comes after it)
    @Test
    fun `test emitListItems with single item`() {
        val result = buildStringBlock {
            block("[", "]") {
                emitListItems(listOf("item1"))
            }
        }

        assertEquals(
            """
            |[
            |    item1
            |]
            |""".trimMargin(),
            result
        )
    }

    // Multiple items: comma added after each item except the last
    @Test
    fun `test emitListItems with multiple items`() {
        val result = buildStringBlock {
            block("[", "]") {
                emitListItems(listOf("item1", "item2", "item3"))
            }
        }

        assertEquals(
            """
            |[
            |    item1,
            |    item2,
            |    item3
            |]
            |""".trimMargin(),
            result
        )
    }

    // Multi-line items: comma is added to the last line of each item (except the final item)
    @Test
    fun `test emitListItems with multiline items`() {
        val result = buildStringBlock {
            block("[", "]") {
                emitListItems(
                    listOf(
                        ".target(\n    name: \"First\"\n)",
                        ".target(\n    name: \"Second\"\n)"
                    )
                )
            }
        }

        assertEquals(
            """
            |[
            |    .target(
            |        name: "First"
            |    ),
            |    .target(
            |        name: "Second"
            |    )
            |]
            |""".trimMargin(),
            result
        )
    }

    // Empty list: no items, no commas
    @Test
    fun `test emitListItems with empty list`() {
        val result = buildStringBlock {
            block("[", "]") {
                emitListItems(emptyList())
            }
        }

        assertEquals(
            """
            |[
            |]
            |""".trimMargin(),
            result
        )
    }

    // --- buildStringBlock ---

    // Default indent is 4 spaces; nested blocks increase indentation level
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

    // Custom indent string (" > ") is used for each indentation level
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

    // Empty block: open and close strings are still emitted
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

    // Empty block between lines: block markers appear between surrounding lines
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

    // Empty block with empty open/close strings: produces no output
    @Test
    fun `test buildStringBlock with empty block, no open or close`() {
        assertEquals(
            "",
            buildStringBlock {
                block("", "") {}
            }
        )
    }

    // Empty block with empty open/close between lines: invisible, lines appear consecutive
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

    // --- connectedLines ---

    // Single line: no suffix added (nothing comes after it)
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

    // Multiple lines: suffix added to each line except the last; continuation lines are indented
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

    // Empty separator: lines are still indented but no suffix is added
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

    // Empty connectedLines block: produces no output
    @Test
    fun `test connectedLines with empty block`() {
        assertEquals(
            "",
            buildStringBlock {
                connectedLines("\\") {}
            }
        )
    }

    // Empty connectedLines between lines: invisible, surrounding lines appear consecutive
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

    // Blank (empty string) lines: suffix is added to empty content, producing just the separator
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

    // Multiple blank lines with backslash suffix between regular lines
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

    // Single blank line: no suffix added (it's the only/last line)
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

    // Multi-line string content: suffix is added after the entire string, not per internal line
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

    // Empty connectedLines inside a block: doesn't affect output
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

    // connectedLines inside a block: inherits block's indentation level
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

    // --- commaSeparatedEntries ---

    // Single entry: no comma added (nothing comes after it)
    @Test
    fun `test commaSeparatedEntries with single entry`() {
        val result = buildStringBlock {
            block("Package(", ")") {
                commaSeparatedEntries {
                    entry { line("name: \"Test\"") }
                }
            }
        }

        assertEquals(
            """
            |Package(
            |    name: "Test"
            |)
            |""".trimMargin(),
            result
        )
    }

    // Multiple entries: comma added after each entry except the last
    @Test
    fun `test commaSeparatedEntries with multiple entries`() {
        val result = buildStringBlock {
            block("Package(", ")") {
                commaSeparatedEntries {
                    entry { line("name: \"Test\"") }
                    entry { line("version: \"1.0\"") }
                    entry { line("author: \"Someone\"") }
                }
            }
        }

        assertEquals(
            """
            |Package(
            |    name: "Test",
            |    version: "1.0",
            |    author: "Someone"
            |)
            |""".trimMargin(),
            result
        )
    }

    // Entries can be multi-line blocks; comma is added to the closing bracket of each block
    @Test
    fun `test commaSeparatedEntries with block entries`() {
        val result = buildStringBlock {
            block("Package(", ")") {
                commaSeparatedEntries {
                    entry { line("name: \"Test\"") }
                    entry {
                        block("platforms: [", "]") {
                            emitListItems(listOf(".iOS(\"15.0\")", ".macOS(\"12.0\")"))
                        }
                    }
                    entry {
                        block("targets: [", "]") {
                            line("\"Main\"")
                        }
                    }
                }
            }
        }

        assertEquals(
            """
            |Package(
            |    name: "Test",
            |    platforms: [
            |        .iOS("15.0"),
            |        .macOS("12.0")
            |    ],
            |    targets: [
            |        "Main"
            |    ]
            |)
            |""".trimMargin(),
            result
        )
    }

    // Empty commaSeparatedEntries: no entries, no commas
    @Test
    fun `test commaSeparatedEntries with empty entries`() {
        val result = buildStringBlock {
            block("Package(", ")") {
                commaSeparatedEntries {
                }
            }
        }

        assertEquals(
            """
            |Package(
            |)
            |""".trimMargin(),
            result
        )
    }

    // Nested commaSeparatedEntries: inner and outer entries get commas independently
    @Test
    fun `test commaSeparatedEntries with nested blocks`() {
        val result = buildStringBlock(defaultIndent = "  ") {
            block("let package = Package(", ")") {
                commaSeparatedEntries {
                    entry { line("name: \"MyPackage\"") }
                    entry {
                        block("products: [", "]") {
                            block(".library(", ")") {
                                commaSeparatedEntries {
                                    entry { line("name: \"MyLib\"") }
                                    entry { line("targets: [\"Main\"]") }
                                }
                            }
                        }
                    }
                    entry {
                        block("targets: [", "]") {
                            line(".target(name: \"Main\")")
                        }
                    }
                }
            }
        }

        assertEquals(
            """
            |let package = Package(
            |  name: "MyPackage",
            |  products: [
            |    .library(
            |      name: "MyLib",
            |      targets: ["Main"]
            |    )
            |  ],
            |  targets: [
            |    .target(name: "Main")
            |  ]
            |)
            |""".trimMargin(),
            result
        )
    }

    // Single multi-line entry: no comma added (only one entry)
    // Inner items use emitListItems for their commas
    @Test
    fun `test commaSeparatedEntries with multiline single entry`() {
        val result = buildStringBlock {
            block("Config(", ")") {
                commaSeparatedEntries {
                    entry {
                        block("settings: [", "]") {
                            emitListItems(listOf("\"a\"", "\"b\""))
                        }
                    }
                }
            }
        }

        assertEquals(
            """
            |Config(
            |    settings: [
            |        "a",
            |        "b"
            |    ]
            |)
            |""".trimMargin(),
            result
        )
    }
}

/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:Suppress("UNUSED_VARIABLE")

package kotlinx.cli

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlin.test.*

class ArgumentsTests {
    @Test
    fun testPositionalArguments() {
        val argParser = ArgParser("testParser")
        val debugMode by argParser.option(ArgType.Boolean, "debug", "d", "Debug mode")
        val input by argParser.argument(ArgType.String, "input", "Input file")
        val output by argParser.argument(ArgType.String, "output", "Output file")
        argParser.parse(arrayOf("-d", "input.txt", "out.txt"))
        assertEquals(true, debugMode)
        assertEquals("out.txt", output)
        assertEquals("input.txt", input)
    }

    @Test
    fun testArgumetsWithAnyNumberOfValues() {
        val argParser = ArgParser("testParser")
        val output by argParser.argument(ArgType.String, "output", "Output file")
        val inputs by argParser.argument(ArgType.String, description = "Input files").vararg()
        argParser.parse(arrayOf("out.txt", "input1.txt", "input2.txt", "input3.txt",
                "input4.txt"))
        assertEquals("out.txt", output)
        assertEquals(4, inputs.size)
    }

    @Test
    fun testArgumetsWithSeveralValues() {
        val argParser = ArgParser("testParser")
        val addendums by argParser.argument(ArgType.Int, "addendums", description = "Addendums").multiple(2)
        val output by argParser.argument(ArgType.String, "output", "Output file")
        val debugMode by argParser.option(ArgType.Boolean, "debug", "d", "Debug mode")
        argParser.parse(arrayOf("2", "-d", "3", "out.txt"))
        assertEquals("out.txt", output)
        val (first, second) = addendums
        assertEquals(2, addendums.size)
        assertEquals(2, first)
        assertEquals(3, second)
    }

    @Test
    fun testSkippingExtraArguments() {
        val argParser = ArgParser("testParser", skipExtraArguments = true)
        val addendums by argParser.argument(ArgType.Int, "addendums", description = "Addendums").multiple(2)
        val output by argParser.argument(ArgType.String, "output", "Output file")
        val debugMode by argParser.option(ArgType.Boolean, "debug", "d", "Debug mode")
        argParser.parse(arrayOf("2", "-d", "3", "out.txt", "something", "else", "in", "string"))
        assertEquals("out.txt", output)
    }
}

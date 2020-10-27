/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlinx.cli

import kotlin.test.*

class OptionsTests {
    @Test
    fun testShortForm() {
        val argParser = ArgParser("testParser")
        val output by argParser.option(ArgType.String, "output", "o", "Output file")
        val input by argParser.option(ArgType.String, "input", "i", "Input file")
        argParser.parse(arrayOf("-o", "out.txt", "-i", "input.txt"))
        assertEquals("out.txt", output)
        assertEquals("input.txt", input)
    }

    @Test
    fun testFullForm() {
        val argParser = ArgParser("testParser")
        val output by argParser.option(ArgType.String, shortName = "o", description = "Output file")
        val input by argParser.option(ArgType.String, shortName = "i", description = "Input file")
        argParser.parse(arrayOf("--output", "out.txt", "--input", "input.txt"))
        assertEquals("out.txt", output)
        assertEquals("input.txt", input)
    }

    @Test
    fun testJavaPrefix() {
        val argParser = ArgParser("testParser", prefixStyle = ArgParser.OptionPrefixStyle.JVM)
        val output by argParser.option(ArgType.String, "output", "o", "Output file")
        val input by argParser.option(ArgType.String, "input", "i", "Input file")
        argParser.parse(arrayOf("-output", "out.txt", "-i", "input.txt"))
        assertEquals("out.txt", output)
        assertEquals("input.txt", input)
    }

    @Test
    fun testGNUPrefix() {
        val argParser = ArgParser("testParser", prefixStyle = ArgParser.OptionPrefixStyle.GNU)
        val output by argParser.option(ArgType.String, "output", "o", "Output file")
        val input by argParser.option(ArgType.String, "input", "i", "Input file")
        val verbose by argParser.option(ArgType.Boolean, "verbose", "v", "Verbose print")
        val shortForm by argParser.option(ArgType.Boolean, "short", "s", "Short output form")
        val text by argParser.option(ArgType.Boolean, "text", "t", "Use text format")
        argParser.parse(arrayOf("-oout.txt", "--input=input.txt", "-vst"))
        assertEquals("out.txt", output)
        assertEquals("input.txt", input)
        assertEquals(verbose, true)
        assertEquals(shortForm, true)
        assertEquals(text, true)
    }

    @Test
    fun testGNUArguments() {
        val argParser = ArgParser("testParser", prefixStyle = ArgParser.OptionPrefixStyle.GNU)
        val output by argParser.argument(ArgType.String, "output", "Output file")
        val input by argParser.argument(ArgType.String, "input", "Input file")
        val verbose by argParser.option(ArgType.Boolean, "verbose", "v", "Verbose print")
        val shortForm by argParser.option(ArgType.Boolean, "short", "s", "Short output form").default(false)
        val text by argParser.option(ArgType.Boolean, "text", "t", "Use text format").default(false)
        argParser.parse(arrayOf("--verbose", "--", "out.txt", "--input.txt"))
        assertEquals("out.txt", output)
        assertEquals("--input.txt", input)
        assertEquals(verbose, true)
        assertEquals(shortForm, false)
        assertEquals(text, false)
    }

    @Test
    fun testMultipleOptions() {
        val argParser = ArgParser("testParser")
        val useShortForm by argParser.option(ArgType.Boolean, "short", "s", "Show short version of report").default(false)
        val renders by argParser.option(ArgType.Choice(listOf("text", "html", "xml", "json")),
                "renders", "r", "Renders for showing information").multiple().default(listOf("text"))
        argParser.parse(arrayOf("-s", "-r", "text", "-r", "json"))
        assertEquals(true, useShortForm)
        assertEquals(2, renders.size)
        val (firstRender, secondRender) = renders
        assertEquals("text", firstRender)
        assertEquals("json", secondRender)
    }

    @Test
    fun testDefaultOptions() {
        val argParser = ArgParser("testParser")
        val useShortForm by argParser.option(ArgType.Boolean, "short", "s", "Show short version of report").default(false)
        val renders by argParser.option(ArgType.Choice(listOf("text", "html", "xml", "json")),
                "renders", "r", "Renders for showing information").multiple().default(listOf("text"))
        val output by argParser.option(ArgType.String, "output", "o", "Output file")
        argParser.parse(arrayOf("-o", "out.txt"))
        assertEquals(false, useShortForm)
        assertEquals("text", renders[0])
    }

    @Test
    fun testResetOptionsValues() {
        val argParser = ArgParser("testParser")
        val useShortFormOption = argParser.option(ArgType.Boolean, "short", "s", "Show short version of report").default(false)
        var useShortForm by useShortFormOption
        val rendersOption = argParser.option(ArgType.Choice(listOf("text", "html", "xml", "json")),
                "renders", "r", "Renders for showing information").multiple().default(listOf("text"))
        var renders by rendersOption
        val outputOption = argParser.option(ArgType.String, "output", "o", "Output file")
        var output by outputOption
        argParser.parse(arrayOf("-o", "out.txt"))
        output = null
        useShortForm = true
        renders = listOf()
        assertEquals(true, useShortForm)
        assertEquals(null, output)
        assertEquals(0, renders.size)
        assertEquals(ArgParser.ValueOrigin.REDEFINED, outputOption.valueOrigin)
        assertEquals(ArgParser.ValueOrigin.REDEFINED, useShortFormOption.valueOrigin)
        assertEquals(ArgParser.ValueOrigin.REDEFINED, rendersOption.valueOrigin)
    }
}

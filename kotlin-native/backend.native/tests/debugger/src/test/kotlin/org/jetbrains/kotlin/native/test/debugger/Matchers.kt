/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.native.test.debugger

import org.intellij.lang.annotations.Language
import org.junit.Assert.fail
import java.nio.file.Files
import java.nio.file.Path

/**
 * An integration test for debug info.
 *
 * It works by compiling a given [programText] with debug info,
 * then launching lldb, feeding it commands from [lldbSession]
 * and matching the output.
 *
 * [lldbSession] specifies both lldb commands and expected output
 * using a DLS, which looks like this:
 *
 *     > b main.kt:5
 *     Breakpoint 1: [..]
 *     > r
 *     Process [..] stopped
 *     [..] at main.kt:5, [..] stop reason = breakpoint [..]
 *     > fr var
 *     (int) a = 92
 *     (int) b = 2
 *
 * It consists of blocks of the form
 *
 *     > lldb command
 *     response line pattern
 *     another pattern
 *
 * Command after `>` is passed to lldb exactly. The output of the command
 * is then matched against a set of patterns. Matching is done line by line:
 * for every pattern, there must be a matching line in the output, but you don't
 * have to specify a pattern for every line. In particular, it's possible not to
 * specify any patterns at all:
 *
 *     > b main.kt:2
 *     > r
 *     > n
 *     [..] at main.kt:3, [..] stop reason = step over
 *
 * The patterns themselves are simple. The only special symbol is `[..]` which
 * means arbitrary substring, which can help match random data, timings, and OS-dependent output.
 * For example, to match
 *
 *     Current executable set to '/tmp/debugger_test7458723719928260513/program.kexe' (x86_64).
 *
 * one writes
 *
 *     Current executable set to [..]program.kexe[..]
 */
fun lldbTest(@Language("kotlin") programText: String, lldbSession: String) {
    lldbReasonToAbort()?.let {
        println(it)
        return
    }

    val lldbSessionSpec = LldbSessionSpecification.parse(lldbSession)

    val tmpdir = Files.createTempDirectory("debugger_test")
    tmpdir.toFile().deleteOnExit()
    val source = tmpdir.resolve("main.kt")
    val output = tmpdir.resolve("program.kexe")

    val driver = ToolDriver()
    Files.write(source, programText.trimIndent().toByteArray())
    driver.compile(source, output, "-g")
    val result = driver.runLldb(output, lldbSessionSpec.commands)
    lldbSessionSpec.match(result)
}

fun lldbReasonToAbort() = when {
    !haveLldb ->
        "Skipping test: no LLDB"
    !targetIsHost() && !simulatorTestEnabled() ->
        "simulator tests disabled, check 'kotlin.native.test.debugger.simulator.enabled' property"
    !isOsxDevToolsEnabled ->
        """Development tools aren't available.
           |Please consider to execute:
           |  ${DistProperties.devToolsSecurity} -enable
           |or
           |  csrutil disable
           |to run lldb tests""".trimMargin()
    else -> null
}

/**
 * Another integration test for debug info.
 *
 * It works by compiling a given set of [src] files with debug info, then
 * launching lldb, running to the given [breakpoint] and "step in" [steps] times.
 * It then checks that none of the reached break points correspond to blank
 * lines in the given source files.
 */
fun lldbCheckLineNumbers(src: Map<String, String>, breakpoint: String, steps: Int) {
    lldbReasonToAbort()?.let {
        println(it)
        return
    }

    val tmpdir = Files.createTempDirectory("debugger_test")
    tmpdir.toFile().deleteOnExit()

    val source = src.map { (filename, content) ->
        val path = tmpdir.resolve(filename)
        Files.write(path, content.trimIndent().toByteArray())
        path
    }.toTypedArray()

    val output = tmpdir.resolve("program.kexe")
    val driver = ToolDriver()
    driver.compile(output, source, "-g")

    val commands = listOf("b ${breakpoint}", "r") + (1..steps).map { "s" } + listOf("q")
    val result = driver.runLldb(output, commands)

    val noCodeLine = Regex("^\\s*(//.*)?$")
    val validSourceBreaks = src.flatMap { (filename, content) ->
        content.lines().withIndex()
                .filterNot { noCodeLine.matches(it.value) }
                .map{ "$filename:${it.index}"}
    }.toSet()

    Regex("(${src.keys.joinToString("|")}):\\d+").findAll(result).forEach {
        check(it.value in validSourceBreaks, { "${it.value} is not a meaningful debug stop" })
    }
}


private val isOsxDevToolsEnabled: Boolean by lazy {
    //TODO: add OSX checks.
    val rawStatus = subprocess(DistProperties.devToolsSecurity, "-status")
    println("> status: $rawStatus")

    val r = Regex("^.*\\ (enabled|disabled).$")
    r.find(rawStatus.stdout)?.destructured?.component1() == "enabled"
}

fun dwarfDumpTest(@Language("kotlin") programText: String, flags: List<String>, test:List<DwarfTag>.()->Unit) {
    if (!haveDwarfDump || !targetIsHost()) {
        println("Skipping test")
        return
    }


    with(Files.createTempDirectory("dwarfdump_test")) {
        toFile().deleteOnExit()
        val source = resolve("main.kt")
        val output = resolve("program.kexe")

        val driver = ToolDriver()
        Files.write(source, programText.trimIndent().toByteArray())
        driver.compile(source, output, "-g", *flags.toTypedArray())
        driver.runDwarfDump(output, processor = test)
    }
}

class ToolDriverHelper(private val driver: ToolDriver, val root:Path) {
    fun String.cinterop(pkg:String, output: String):Path {
        val def = feedOutput("$output.def")
        val lib = root.resolve("$output.klib")
        driver.cinterop(def, lib, pkg)
        return lib
    }


    fun String.library(output: String, vararg flags:String) = feedOutput("$output.kt").compile(root.resolve("$output.klib"), "-p", "library", *flags)

    fun String.binary(output: String, vararg flags:String)= feedOutput("$output.kt").compile(root.resolve("$output.kexe"), *flags)

    private fun Path.compile(output: Path, vararg flags:String) = output.also{ driver.compile(source = this, it, *flags) }

    fun Path.dwarfDumpLookup(address: Long, parser:List<DwarfTag>.() -> Unit) = driver.runDwarfDump(this, "-lookup", address.toString(), processor = parser)
    fun Path.dwarfDumpLookup(name: String, parser:List<DwarfTag>.() -> Unit) = driver.runDwarfDump(this, "-find", name, processor = parser)


    fun String.feedOutput(output: String) = root.resolve(output).also {
            Files.write(it, this.trimIndent().toByteArray())
        }

    fun Array<Path>.framework(name:String, vararg args:String = emptyArray()):Path = root.resolve("$name.framework").also {

        driver.compile(it, this, "-produce", "framework", *args)
    }

    fun Array<Path>.binary(name:String, vararg args:String = emptyArray()):Path = root.resolve("$name.kexe").also {
        driver.compile(it, this, *args)
    }

    fun swiftc(output: String, swiftSrc: Path, vararg args: String) = root.resolve(output).also {
        driver.swiftc(it, swiftSrc, *args, "-Xlinker", "-rpath", "-Xlinker", "@executable_path")
    }

    fun String.lldb(program:Path) {
        val lldbSessionSpec = LldbSessionSpecification.parse(this)
        val result = driver.runLldb(program, lldbSessionSpec.commands)
        lldbSessionSpec.match(result)
    }

}

fun dwarfDumpComplexTest(test:ToolDriverHelper.()->Unit) {
    if (!haveDwarfDump || !targetIsHost()) {
        println("Skipping test")
        return
    }


    with(Files.createTempDirectory("dwarfdump_test_complex")) {
        toFile().deleteOnExit()
        ToolDriverHelper(ToolDriver(), this).test()
    }
}

fun lldbComplexTest(test:ToolDriverHelper.()->Unit) {
    if (!haveLldb || !targetIsHost()) {
        println("Skipping test")
        return
    }


    with(Files.createTempDirectory("lldb_test_complex")) {
        toFile().deleteOnExit()
        ToolDriverHelper(ToolDriver(), this).test()
    }
}

private class LldbSessionSpecification private constructor(
        val commands: List<String>,
        val patterns: List<List<String>>
) {

    fun match(output: String) {
        val blocks = output.split("""(?=\(lldb\))""".toRegex()).filterNot(String::isEmpty)
        if (targetIsHost()) {
            check(blocks[0].startsWith("(lldb) target create")) { "Missing block \"target create\". Got: ${blocks[0]}" }
            check(blocks[1].startsWith("(lldb) command script import")) {
                "Missing block \"command script import\". Got: ${blocks[0]}"
            }
        }
        val responses = if (targetIsHost())
            blocks.drop(2)
        else
            blocks.drop(2).dropLast(1)

        val executedCommands = responses.map { it.lines().first() }
        val bodies = responses.map { it.lines().drop(1) }
        val responsesMatch = executedCommands.size == commands.size
                && commands.zip(executedCommands).all { (cmd, h) -> h == "(lldb) $cmd" }

        if (!responsesMatch) {
            val message = """
                |Responses do not match commands.
                |
                |COMMANDS: |$commands (${commands.size})
                |RESPONSES: |$executedCommands (${executedCommands.size})
                |
                |FULL SESSION:
                |$output
            """.trimMargin()
            fail(message)
        }

        for ((patternBody, command) in patterns.zip(bodies).zip(executedCommands)) {
            val (pattern, body) = patternBody
            val mismatch = findMismatch(pattern, body)
            if (mismatch != null) {
                val message = """
                    |Wrong LLDB output.
                    |
                    |COMMAND: $command
                    |PATTERN: $mismatch
                    |OUTPUT:
                    |${body.joinToString("\n")}
                    |
                    |FULL SESSION:
                    |$output
                """.trimMargin()
                fail(message)
            }
        }
    }

    private fun findMismatch(patterns: List<String>, actualLines: List<String>): String? {
        val indices = mutableListOf<Int>()
        for (pattern in patterns) {
            val idx = actualLines.indexOfFirst { match(pattern, it) }
            if (idx == -1) {
                return pattern
            }
            indices += idx
        }
        check(indices == indices.sorted())
        return null
    }

    private fun match(pattern: String, line: String): Boolean {
        val chunks = pattern.split("""\s*\[\.\.]\s*""".toRegex())
                .filter { it.isNotBlank() }
                .map { it.trim() }
        check(chunks.isNotEmpty())
        val trimmedLine = line.trim()

        val indices = chunks.map { trimmedLine.indexOf(it) }
        if (indices.any { it == -1 } || indices != indices.sorted()) return false
        if (!(trimmedLine.startsWith(chunks.first()) || pattern.startsWith("[..]"))) return false
        if (!(trimmedLine.endsWith(chunks.last()) || pattern.endsWith("[..]"))) return false
        return true
    }

    companion object {
        fun parse(spec: String): LldbSessionSpecification {
            val blocks = spec.trimIndent()
                    .split("(?=^>)".toRegex(RegexOption.MULTILINE))
                    .filterNot(String::isEmpty)
            for (cmd in blocks) {
                check(cmd.startsWith(">")) { "Invalid lldb session specification: $cmd" }
            }
            val commands = blocks.map { it.lines().first().substring(1).trim() }
            val patterns = blocks.map { it.lines().drop(1).filter { it.isNotBlank() } }
            return LldbSessionSpecification(commands, patterns)
        }
    }
}

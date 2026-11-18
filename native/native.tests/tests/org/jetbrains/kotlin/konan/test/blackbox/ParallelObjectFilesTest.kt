/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.callCompiler
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ForcedNoopTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PlatformLibs
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.provisionedXcodeCompilerArgs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Black-box coverage for parallel object emission (`objectFileThreads` + in-process emit).
 *
 * Contract checks beyond smoke:
 * - serial vs parallel runtime behavior (init / virtual / exceptions)
 * - linker GC retains needed symbols (`--gc-sections` / Darwin dead_strip)
 * - STATIC + ld -r keeps a single archive member
 * - Mach-O framework path still emits ObjC metadata under split
 * - happy path must not fall back to serial (e.g. unnamed locals)
 * - welded metadata/globals still allow multi-part emission
 * - multiple source files still split across parts
 */
@EnforcedHostTarget
class ParallelObjectFilesTest : AbstractNativeSimpleTest() {
    @BeforeEach
    fun requireExecutableUnixHost() {
        assumeTrue(
                HostManager.host.family == Family.OSX ||
                        HostManager.host.family == Family.LINUX
        )
        assumeFalse(testRunSettings.get<ForcedNoopTestRunner>().value)
    }

    @Test
    fun oneThreadUsesOneObject() = compileProgramAndCheck(1, richSource())

    @Test
    fun twoThreadsUseTwoObjects() = compileProgramAndCheck(2, richSource())

    @Test
    fun fourThreadsUseFourObjects() = compileProgramAndCheck(4, richSource())

    @Test
    fun eightThreadsUseEightObjects() = compileProgramAndCheck(8, richSource())

    @Test
    fun serialAndParallelProduceSameOutput() {
        val serial = compileProgram(threads = 1, label = "serial", sourceText = richSource())
        val parallel = compileProgram(threads = 4, label = "parallel", sourceText = richSource())
        assertEquals(EXPECTED_RICH_OUTPUT, runAndCapture(serial.executable))
        assertEquals(EXPECTED_RICH_OUTPUT, runAndCapture(parallel.executable))
        assertEquals(4, parallel.objects.size) { parallel.toolOutput }
        assertNoSerialFallback(parallel.toolOutput)
        if (HostManager.host.family == Family.LINUX) {
            assertNoDuplicateGlobalDefs(parallel.objects)
            assertKnRenamesPresentWhenSplit(parallel.objects)
        }
    }

    /**
     * Objects / static init pull globals into the Mach-O same-file weld; that must not
     * collapse emission to a single serial object or trigger fallback.
     */
    @Test
    fun weldedGlobalsStillEmitMultipleParts() {
        val threads = 4
        val result = compileProgram(threads, "welded-globals", richSource())
        assertEquals(threads, result.objects.size) { result.toolOutput }
        assertNoSerialFallback(result.toolOutput)
        result.objects.forEach(::assertObjectFile)
        assertEquals(EXPECTED_RICH_OUTPUT, runAndCapture(result.executable))
    }

    /**
     * Multiple DI files: remaining functions are grouped per source file on Mach-O.
     * Parallelism must remain (multi-part) without serial fallback.
     */
    @Test
    fun multiFileSourcesStayParallelWithoutFallback() {
        val threads = 4
        val work = buildDir.resolve("multi-file").apply { mkdirs() }
        val temporaryFiles = work.resolve("codegen").apply { mkdirs() }
        val fileA = work.resolve("a.kt").apply {
            writeText(
                    """
                fun fromA(): Int = 40
                private fun helperA(): String = "A"
                fun tagA(): String = helperA()
            """.trimIndent()
            )
        }
        val fileB = work.resolve("b.kt").apply {
            writeText(
                    """
                fun fromB(): Int = 2
                private fun helperB(): String = "B"
                fun tagB(): String = helperB()
            """.trimIndent()
            )
        }
        val main = work.resolve("main.kt").apply {
            writeText(
                    """
                fun main() {
                    check(fromA() + fromB() == 42)
                    check(tagA() + tagB() == "AB")
                    println("OK:MULTI")
                }
            """.trimIndent()
            )
        }
        val executable = work.resolve("main.kexe")
        val compilerArgs = buildList {
            addAll(listOf(fileA.path, fileB.path, main.path))
            addAll(listOf("-target", HostManager.host.name, "-o", executable.path, "-opt"))
            add("-Xin-process-obj-emission")
            add("-Xbinary=objectFileThreads=$threads")
            add("-Xtemporary-files-dir=${temporaryFiles.path}")
            testRunSettings.get<PlatformLibs>().compilerFlag?.let { add(it) }
            addAll(provisionedXcodeCompilerArgs)
        }.toTypedArray()
        val result = callCompiler(
                compilerArgs = compilerArgs,
                kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>().classLoader,
        )
        assertEquals(ExitCode.OK, result.exitCode) {
            "Multi-file compilation failed: ${compilerArgs.joinToString(" ")}\n${result.toolOutput}"
        }
        assertNoSerialFallback(result.toolOutput)
        val objects = temporaryFiles.listFiles().orEmpty().filter { it.extension == "o" && "-part" in it.name }
        assertEquals(threads, objects.size) {
            "Expected $threads parts for multi-file parallel emit; got ${objects.map { it.name }}\n${result.toolOutput}"
        }
        objects.forEach(::assertObjectFile)
        assertEquals("OK:MULTI", runAndCapture(executable))
        if (HostManager.host.family == Family.LINUX) {
            assertNoDuplicateGlobalDefs(objects)
            assertKnRenamesPresentWhenSplit(objects)
        }
    }

    /** Init order, virtual calls, and exceptions must match with linker GC enabled. */
    @Test
    fun serialAndParallelMatchWithLinkerGc() {
        val extra = when (HostManager.host.family) {
            Family.LINUX -> listOf("-linker-option", "-Wl,--gc-sections")
            Family.OSX -> listOf("-linker-option", "-dead_strip")
            else -> emptyList()
        }
        val serial = compileProgram(1, "gc-serial", richSource(), extra)
        val parallel = compileProgram(4, "gc-parallel", richSource(), extra)
        assertEquals(EXPECTED_RICH_OUTPUT, runAndCapture(serial.executable))
        assertEquals(EXPECTED_RICH_OUTPUT, runAndCapture(parallel.executable))
    }

    /**
     * STATIC / STATIC_CACHE path: parallel parts are relocate-linked (`ld -r`) into one member
     * before ar/libtool. Part `.o` files may remain in the temp dir; the archive must not contain
     * multiple split members.
     */
    @Test
    fun staticArchiveGetsSingleRelocatedMember() {
        val threads = 4
        val work = buildDir.resolve("static-lib").apply { mkdirs() }
        val source = work.resolve("lib.kt").apply {
            writeText(
                    """
                package parallellib
                object Counter {
                    var value = 0
                    fun bump(): Int = ++value
                }
                fun answer(): Int = Counter.bump() + 41
            """.trimIndent()
            )
        }
        val library = work.resolve("libparallel")
        val temporaryFiles = work.resolve("codegen").apply { mkdirs() }
        val compilerArgs = baseCompilerArgs(
                source = source,
                output = library,
                threads = threads,
                temporaryFiles = temporaryFiles,
                extra = listOf(
                        "-produce", "static",
                        "-Xbinary=cInterfaceMode=v1",
                ),
        )
        val result = callCompiler(
                compilerArgs = compilerArgs,
                kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>().classLoader,
        )
        assertEquals(ExitCode.OK, result.exitCode) {
            "STATIC compilation failed: ${compilerArgs.joinToString(" ")}\n${result.toolOutput}"
        }
        assertNoSerialFallback(result.toolOutput)

        val parts = temporaryFiles.listFiles().orEmpty().filter { it.extension == "o" && "-part" in it.name }
        assertEquals(threads, parts.size) {
            "Expected $threads parallel part objects before/alongside ld -r; got ${parts.map { it.name }}\n${result.toolOutput}"
        }
        parts.forEach(::assertObjectFile)

        val archive = findStaticArchive(work, library)
        assertTrue(archive.isFile) { "Missing static archive near $library; files=${work.listFiles()?.map { it.name }}" }
        val objectMembers = archiveObjectMembers(archive)
        assertTrue(objectMembers.isNotEmpty()) {
            "STATIC archive has no object members: $archive\n${result.toolOutput}"
        }
        assertTrue(objectMembers.none { "-part" in it }) {
            "STATIC+parallel must ld -r before ar; archive must not contain split part members: $objectMembers\n${result.toolOutput}"
        }
        // Prefer a single relocated object; allow toolchain extras (e.g. symdef) that are not .o parts.
        val codeMembers = objectMembers.filter { it.endsWith(".o") || it.endsWith(".obj") }
        assertEquals(1, codeMembers.size) {
            "Expected one relocated code member in STATIC archive; members=$objectMembers\n${result.toolOutput}"
        }
    }

    /** Mach-O Keep contract smoke: framework split still emits ObjC ABI metadata. */
    @Test
    fun machOFrameworkSplitKeepsObjCMetadata() {
        assumeTrue(HostManager.host.family == Family.OSX)

        fun buildFramework(threads: Int, label: String): File {
            val work = buildDir.resolve("framework-$label").apply { mkdirs() }
            val source = work.resolve("lib.kt").apply {
                writeText(
                        """
                    class Exported(val n: Int) {
                        fun doubled(): Int = n * 2
                    }
                    fun makeExported(n: Int) = Exported(n)
                """.trimIndent()
                )
            }
            val output = work.resolve("ParallelKeep")
            val temporaryFiles = work.resolve("codegen").apply { mkdirs() }
            val compilerArgs = baseCompilerArgs(
                    source = source,
                    output = output,
                    threads = threads,
                    temporaryFiles = temporaryFiles,
                    extra = listOf(
                            "-produce", "framework",
                            "-Xbinary=bundleId=ParallelKeep",
                    ),
            )
            val result = callCompiler(
                    compilerArgs = compilerArgs,
                    kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>().classLoader,
            )
            assertEquals(ExitCode.OK, result.exitCode) {
                "Framework compilation failed (threads=$threads): ${compilerArgs.joinToString(" ")}\n${result.toolOutput}"
            }
            if (threads > 1) {
                assertNoSerialFallback(result.toolOutput)
                val parts = temporaryFiles.listFiles().orEmpty().filter { it.extension == "o" && "-part" in it.name }
                assertEquals(threads, parts.size) {
                    "Framework parallel emit fell back or under-split; parts=${parts.map { it.name }}\n${result.toolOutput}"
                }
                parts.forEach(::assertMachOObject)
            }
            val binary = work.resolve("ParallelKeep.framework/ParallelKeep")
            assertTrue(binary.isFile) { "Missing framework binary at $binary" }
            return binary
        }

        val serial = buildFramework(1, "serial")
        val parallel = buildFramework(4, "parallel")
        val serialObjC = nmAllSymbols(serial).filter { it.contains("OBJC_") || it.contains("objc_") }
        val parallelObjC = nmAllSymbols(parallel).filter { it.contains("OBJC_") || it.contains("objc_") }
        assertTrue(serialObjC.isNotEmpty()) { "Serial framework missing ObjC metadata symbols" }
        assertTrue(parallelObjC.isNotEmpty()) {
            "Parallel framework missing ObjC metadata symbols (Keep regression?); sample=${nmAllSymbols(parallel).take(20)}"
        }
    }

    private data class CompileResult(
            val executable: File,
            val objects: List<File>,
            val toolOutput: String,
    )

    private fun compileProgramAndCheck(threads: Int, sourceText: String) {
        val result = compileProgram(threads, "t$threads", sourceText)
        assertEquals(if (threads == 1) 0 else threads, result.objects.size) { result.toolOutput }
        result.objects.forEach(::assertObjectFile)
        assertTrue(result.executable.isFile)
        assertEquals(EXPECTED_RICH_OUTPUT, runAndCapture(result.executable))
        if (threads > 1 && HostManager.host.family == Family.LINUX) {
            assertNoDuplicateGlobalDefs(result.objects)
        }
    }

    private fun compileProgram(
            threads: Int,
            label: String,
            sourceText: String,
            extra: List<String> = emptyList(),
    ): CompileResult {
        val work = buildDir.resolve(label).apply { mkdirs() }
        val source = work.resolve("main.kt").apply { writeText(sourceText) }
        val executable = work.resolve("main.kexe")
        val temporaryFiles = work.resolve("codegen").apply { mkdirs() }
        val compilerArgs = baseCompilerArgs(
                source = source,
                output = executable,
                threads = threads,
                temporaryFiles = temporaryFiles,
                extra = extra,
        )
        val result = callCompiler(
                compilerArgs = compilerArgs,
                kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>().classLoader,
        )
        assertEquals(ExitCode.OK, result.exitCode) {
            "Compilation failed: ${compilerArgs.joinToString(" ")}\n${result.toolOutput}"
        }
        if (threads > 1) {
            assertNoSerialFallback(result.toolOutput)
        }
        val objects = temporaryFiles.listFiles().orEmpty().filter { it.extension == "o" && "-part" in it.name }
        assertTrue(temporaryFiles.listFiles().orEmpty().none { it.extension == "bc" && "-part" in it.name })
        return CompileResult(executable, objects, result.toolOutput)
    }

    private fun assertNoSerialFallback(toolOutput: String) {
        assertTrue("Falling back to serial object emission" !in toolOutput) {
            "Parallel object emission unexpectedly fell back to serial:\n$toolOutput"
        }
    }

    private fun baseCompilerArgs(
            source: File,
            output: File,
            threads: Int,
            temporaryFiles: File,
            extra: List<String> = emptyList(),
    ): Array<String> = buildList {
        addAll(listOf(source.path, "-target", HostManager.host.name, "-o", output.path, "-opt"))
        add("-Xin-process-obj-emission")
        add("-Xbinary=objectFileThreads=$threads")
        add("-Xtemporary-files-dir=${temporaryFiles.path}")
        addAll(extra)
        testRunSettings.get<PlatformLibs>().compilerFlag?.let { add(it) }
        addAll(provisionedXcodeCompilerArgs)
    }.toTypedArray()

    private fun assertObjectFile(file: File) {
        when (HostManager.host.family) {
            Family.OSX -> assertMachOObject(file)
            Family.LINUX -> assertElfObject(file)
            else -> error("unsupported host family")
        }
    }

    private fun assertMachOObject(file: File) {
        val bytes = ByteArray(16)
        java.io.DataInputStream(file.inputStream()).use { it.readFully(bytes) }
        val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0xfeedfacf.toInt(), header.getInt(0), "Expected a 64-bit Mach-O file: $file")
        assertEquals(1, header.getInt(12), "Expected MH_OBJECT: $file")
    }

    private fun assertElfObject(file: File) {
        val bytes = ByteArray(4)
        java.io.DataInputStream(file.inputStream()).use { it.readFully(bytes) }
        assertEquals(0x7f, bytes[0].toInt() and 0xff, "Expected ELF magic: $file")
        assertEquals('E'.code, bytes[1].toInt() and 0xff, "Expected ELF magic: $file")
        assertEquals('L'.code, bytes[2].toInt() and 0xff, "Expected ELF magic: $file")
        assertEquals('F'.code, bytes[3].toInt() and 0xff, "Expected ELF magic: $file")
    }

    private fun assertNoDuplicateGlobalDefs(objects: List<File>) {
        val defined = linkedMapOf<String, String>()
        for (obj in objects) {
            for (name in nmDefinedGlobals(obj)) {
                if (name.startsWith("__kn_codegen_anchor_")) continue
                val previous = defined.put(name, obj.name)
                assertTrue(previous == null) {
                    "Duplicate global definition '$name' in $previous and ${obj.name}"
                }
            }
        }
    }

    private fun assertKnRenamesPresentWhenSplit(objects: List<File>) {
        val all = objects.flatMap { nmDefinedGlobals(it) }
        assertTrue(all.any { ".kn." in it }) {
            "Expected at least one .kn.<moduleId> renamed local across partitions; got: $all"
        }
    }

    private fun findStaticArchive(work: File, outputBase: File): File {
        val candidates = listOf(
                work.resolve("lib${outputBase.name}.a"),
                outputBase.resolveSibling("lib${outputBase.name}.a"),
                File(outputBase.path + ".a"),
                outputBase,
        )
        candidates.firstOrNull { it.isFile && (it.extension == "a" || it.name.endsWith(".a")) }?.let { return it }
        return work.walkTopDown().firstOrNull { it.isFile && it.extension == "a" }
                ?: error("No .a produced under $work; tried $candidates")
    }

    private fun archiveObjectMembers(archive: File): List<String> {
        val process = ProcessBuilder("ar", "t", archive.path)
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        assertEquals(0, code) { "ar t failed for $archive:\n$output" }
        return output.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && (it.endsWith(".o") || it.endsWith(".obj")) }
                .toList()
    }

    private fun nmDefinedGlobals(obj: File): List<String> {
        val args = when (HostManager.host.family) {
            Family.LINUX -> listOf("nm", "-g", "--defined-only", obj.path)
            else -> listOf("nm", "-g", "-U", obj.path)
        }
        return runNm(args)
    }

    private fun nmAllSymbols(file: File): List<String> {
        val args = when (HostManager.host.family) {
            Family.LINUX -> listOf("nm", "-g", file.path)
            else -> listOf("nm", "-g", file.path)
        }
        return runNm(args)
    }

    private fun runNm(args: List<String>): List<String> {
        val process = ProcessBuilder(args).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        assertEquals(0, code) { "nm failed (${args.joinToString(" ")}):\n$output" }
        return output.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { line ->
                    val parts = line.split(Regex("\\s+"))
                    when {
                        parts.size >= 3 && parts[1].length == 1 -> parts.last()
                        parts.size == 2 -> parts.last() // macOS sometimes: "U _foo"
                        else -> null
                    }
                }
                .toList()
    }

    private fun runAndCapture(executable: File): String {
        var captured = ""
        runExecutableAndVerify(
                testCase = TestCase(
                        id = TestCaseId.Named("main"),
                        kind = TestKind.STANDALONE_NO_TR,
                        modules = emptySet(),
                        freeCompilerArgs = TestCompilerArgs.EMPTY,
                        nominalPackageName = PackageName.EMPTY,
                        checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).copy(
                                outputMatcher = TestRunCheck.OutputMatcher { output ->
                                    captured = output.trim()
                                    true
                                },
                        ),
                        extras = TestCase.NoTestRunnerExtras(entryPoint = "main"),
                ),
                executable = TestExecutable(
                        executable = TestCompilationArtifact.Executable(executable),
                        loggedCompilationToolCall = LoggedData.NoopCompilerCall(executable),
                        testNames = listOf(TestName("main")),
                ),
        )
        return captured
    }

    companion object {
        private const val EXPECTED_RICH_OUTPUT = "OK:BA:2:expected"

        private fun richSource(): String = """
            val log = StringBuilder()
            object A {
                init { log.append('A') }
            }
            object B {
                init { log.append('B') }
                val trigger = A
            }
            open class Base {
                open fun kind(): Int = 1
            }
            class Derived : Base() {
                override fun kind(): Int = 2
            }
            private var state = 0
            private fun first(): Int = ++state
            private fun second(): Int = ++state
            fun main(args: Array<String>) {
                B
                check(log.toString() == "BA")
                val base: Base = Derived()
                check(base.kind() == 2)
                val functions = if (args.isEmpty()) listOf(::first, ::second) else listOf(::second, ::first)
                check(functions[0]() == 1)
                check(functions[1]() == 2)
                var message = "missing"
                try {
                    error("expected")
                } catch (e: IllegalStateException) {
                    message = e.message ?: "null"
                }
                check(message == "expected")
                println("OK:${'$'}log:${'$'}{base.kind()}:${'$'}message")
            }
        """.trimIndent()
    }
}

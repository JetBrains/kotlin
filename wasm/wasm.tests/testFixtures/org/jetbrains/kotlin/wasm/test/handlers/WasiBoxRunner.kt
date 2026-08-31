/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.WasmCompilationSetsBinaryArtifact
import org.jetbrains.kotlin.test.model.WasmFolderBinaryArtifact
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator.Companion.WASM_BASE_FILE_NAME
import org.jetbrains.kotlin.test.services.configuration.useNewExceptionHandling
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import org.jetbrains.kotlin.wasm.test.tools.WasmVmDescriptor
import java.io.File
import java.io.InputStream

/**
 * The `test.mjs` launcher for WASI unit-test and grouped runs under Node.js, exiting with code 1 on any uncaught
 * exception (e.g. a hard VM trap). WasmEdge and Wasmtime bypass this script and invoke the artifact's `startTest`
 * export directly (see [WasmVM.WasmEdge] and [WasmVM.Wasmtime]).
 *
 * [callGroupedTestsDriver] must come from the artifact the stage-2 facade produced, not from probing the exports:
 * `wasiBoxTestRun.kt` exports a `startTest()` of its own that merely runs `box()`, so a probe would run `box()` in
 * place of `startUnitTests()` for an isolated `// RUN_UNIT_TESTS` test that also has a `box()`.
 */
fun startUnitTestsWasiScript(callGroupedTestsDriver: Boolean): String = """
    try {
        let jsModule = await import('./$WASM_BASE_FILE_NAME.mjs');
        ${if (callGroupedTestsDriver) "jsModule.startTest();" else "jsModule.startUnitTests();"}
    } catch(e) {
        console.log('Failed with exception!');
        console.log(e);
        process.exit(1);
    }
    """.trimIndent()

private const val WASM_EXPORT_SECTION_ID = 7
private const val WASM_FUNCTION_EXPORT_KIND = 0
private const val WASM_SKIP_BUFFER_SIZE = 8 * 1024L
private const val WASM_BINARY_MAGIC = 0x6D736100L
private const val WASM_BINARY_VERSION = 1L
private val REQUIRED_WASM_EXPORT_NAMES = listOf("runBoxTest", "startTest")
private val REQUIRED_WASM_EXPORT_NAME_BYTES = REQUIRED_WASM_EXPORT_NAMES.associateWith { it.toByteArray(Charsets.UTF_8) }

/** The export inspector only needs a small linker-produced section and a few short names. */
internal const val MAX_WASM_EXPORT_SECTION_SIZE = 64 * 1024 * 1024L
internal const val MAX_WASM_EXPORT_NAME_SIZE = 1024 * 1024L

internal fun readWasmExportNames(wasmFile: File): Set<String> = try {
    wasmFile.inputStream().buffered().use { input ->
        // Bounding the top-level reader by the file length keeps every declared size — a section's, and an export
        // name's inside it — checked against bytes that actually exist. Left unbounded, `requireAvailable` would wave
        // every size through, and a corrupt one would reach `ByteArray(size.toInt())`
        // in `readBytes`: an allocation of up to 2 GB whose `OutOfMemoryError` is an `Error`, so it escapes the
        // `catch` below and takes the test JVM down instead of being reported as framing that cannot be read.
        val reader = WasmBinaryReader(input, remaining = wasmFile.length())
        require(reader.readUInt32() == WASM_BINARY_MAGIC) { "Invalid Wasm binary magic" }
        require(reader.readUInt32() == WASM_BINARY_VERSION) { "Unsupported Wasm binary version" }

        var exportedNames: Set<String>? = null
        var reachedEnd = false
        while (exportedNames == null && !reachedEnd) {
            val sectionId = reader.readByteOrNull()
            if (sectionId == null) {
                reachedEnd = true
            } else {
                val sectionSize = reader.readVarUInt32()
                if (sectionId == WASM_EXPORT_SECTION_ID) {
                    exportedNames = reader.readExportNames(sectionSize)
                } else {
                    reader.skip(sectionSize)
                }
            }
        }
        exportedNames ?: emptySet()
    }
} catch (e: Exception) {
    testInfraError("Failed to read Wasm exports from ${wasmFile.absolutePath}: ${e.message}")
}

/** Reads Wasm section framing and export entries without parsing the rest of the module. */
private class WasmBinaryReader(
    private val input: InputStream,
    private var remaining: Long,
) {
    fun readByteOrNull(): Int? {
        if (remaining == 0L) return null

        val value = input.read()
        if (value < 0) {
            error("Unexpected end of Wasm section")
        }

        remaining--
        return value
    }

    private fun readByte(): Int = readByteOrNull() ?: error("Unexpected end of Wasm binary")

    fun readUInt32(): Long {
        var result = 0L
        repeat(4) { byteIndex ->
            result = result or (readByte().toLong() shl (byteIndex * 8))
        }
        return result
    }

    /**
     * Deliberately not [org.jetbrains.kotlin.utils.readUnsignedLeb128]: that shared decoder does not reject a fifth
     * byte whose value exceeds the 4 bits a 32-bit LEB128 has room for in it. Such a byte just gets OR'd in and
     * shifted by 28, and `UInt.shl` does not throw on overflow — it silently drops any bit that lands at position 32
     * or beyond, so e.g. the 5-byte sequence `80 80 80 80 10` decodes there to `0u` instead of failing. Reusing it
     * here would let a corrupted section-size byte silently misread as zero rather than surfacing the corruption,
     * which is the one thing this reader exists to catch — hence the extra `shift == 28` check below.
     */
    fun readVarUInt32(): Long {
        var result = 0L
        for (shift in 0..28 step 7) {
            val byte = readByte()
            val value = byte and 0x7F
            if (shift == 28 && value > 0x0F) {
                error("Invalid unsigned 32-bit LEB128 number")
            }
            result = result or (value.toLong() shl shift)
            if (byte and 0x80 == 0) return result
        }
        error("Invalid unsigned 32-bit LEB128 number")
    }

    fun readExportNames(sectionSize: Long): Set<String> {
        require(sectionSize <= MAX_WASM_EXPORT_SECTION_SIZE) {
            "Wasm export section is too large ($sectionSize bytes; maximum is $MAX_WASM_EXPORT_SECTION_SIZE)"
        }
        // The sub-reader adopts [sectionSize] as its whole budget, so an oversized one has to be rejected against
        // this reader's own bound first: every length inside the section is then checked against bytes that exist.
        requireAvailable(sectionSize)

        val section = WasmBinaryReader(input, sectionSize)
        val exportCount = section.readVarUInt32()
        val names = mutableSetOf<String>()

        var exportIndex = 0L
        while (exportIndex < exportCount && names.size < REQUIRED_WASM_EXPORT_NAMES.size) {
            val name = section.readRequiredExportNameOrNull()
            val exportKind = section.readByte()
            section.readVarUInt32() // export index
            if (exportKind == WASM_FUNCTION_EXPORT_KIND && name != null) {
                names += name
            }
            exportIndex++
        }

        if (names.size == REQUIRED_WASM_EXPORT_NAMES.size) {
            // The export surface needed by the caller is complete. Consume the bounded remainder without decoding
            // unrelated names, so a large valid export table cannot retain one String per export.
            section.skip(section.remaining)
        }
        section.requireFullyConsumed()
        // `section` tracks its own budget independently of this reader's `remaining`, even though both read from the
        // same shared `input`. Without this, `remaining` would go stale by exactly `sectionSize` the moment this
        // returns — currently harmless, since the only caller stops scanning as soon as the export section is found,
        // but silent staleness is exactly the kind of bug the file-length bound elsewhere in this class exists to
        // prevent, so it is closed here too rather than left for a future scan past this section to reintroduce it.
        consume(sectionSize)
        return names
    }

    /** Reads a required export name only; unrelated names are validated and skipped without allocating their bytes. */
    private fun readRequiredExportNameOrNull(): String? {
        val size = readVarUInt32()
        val requiredName = REQUIRED_WASM_EXPORT_NAME_BYTES.entries.firstOrNull { it.value.size.toLong() == size }
        if (requiredName == null) {
            skip(size)
            return null
        }

        val bytes = readBytes(size)
        return requiredName.key.takeIf { bytes.contentEquals(requiredName.value) }
    }

    private fun readBytes(size: Long): ByteArray {
        require(size <= MAX_WASM_EXPORT_NAME_SIZE) {
            "Wasm export name is too large ($size bytes; maximum is $MAX_WASM_EXPORT_NAME_SIZE)"
        }
        requireAvailable(size)

        val bytes = ByteArray(size.toInt())
        var offset = 0
        while (offset < bytes.size) {
            val read = input.read(bytes, offset, bytes.size - offset)
            when {
                read < 0 -> error("Unexpected end of Wasm binary")
                read == 0 -> bytes[offset++] = readByte().toByte()
                else -> {
                    consume(read.toLong())
                    offset += read
                }
            }
        }
        return bytes
    }

    /**
     * Reads the bytes away instead of seeking over them: `InputStream.skip` is free to move past the end of the file,
     * which would turn a truncated module into a silent "this module has no exports" and, in turn, into a report that
     * the binary does not export `startTest`. Bounded against [remaining] up front, like [readBytes], so a section
     * whose declared size overruns its parent is rejected before anything is read out of the shared stream.
     */
    fun skip(size: Long) {
        require(size >= 0) { "Cannot skip a negative number of bytes" }
        requireAvailable(size)

        val buffer = ByteArray(minOf(size, WASM_SKIP_BUFFER_SIZE).toInt())
        var left = size
        while (left > 0) {
            val read = input.read(buffer, 0, minOf(left, buffer.size.toLong()).toInt())
            if (read <= 0) error("Unexpected end of Wasm binary: a section declares more bytes than the file holds")
            consume(read.toLong())
            left -= read
        }
    }

    /** Shared by [readBytes] and [skip]: a size that overruns the enclosing section is corrupt framing either way. */
    private fun requireAvailable(size: Long) {
        require(size >= 0) { "Negative Wasm byte vector size" }
        if (size > remaining) {
            error("A Wasm section declares more bytes ($size) than remain in its enclosing section ($remaining)")
        }
    }

    private fun consume(size: Long) {
        require(size <= remaining) { "Read past the end of a Wasm section" }
        remaining -= size
    }

    private fun requireFullyConsumed() {
        require(remaining == 0L) { "Wasm export section contains trailing bytes" }
    }
}

/**
 * Turns the linker-level invariant the bare `startTest` export relies on into a loud failure: should a
 * `wasiBoxTestRun.kt` helper ever reach a grouped binary, `startTest` may no longer resolve to the driver and the
 * batch would run a single `box()` instead. See `WasmWasiGroupedTestsExportedEntryPointGenerator`.
 */
internal fun assertDriverOwnsStartTestExport(dir: File) {
    // A mode that produced no output folder at all is not this check's business; a folder without the binary is.
    if (!dir.isDirectory) return

    val wasmFile = dir.resolve("$WASM_BASE_FILE_NAME.wasm")
    if (!wasmFile.exists()) {
        testInfraError(
            "A driver-linked grouped batch left no `$WASM_BASE_FILE_NAME.wasm` in ${dir.absolutePath}, so the " +
                    "invariant that the bare `startTest` export belongs to the result-collecting driver cannot be " +
                    "checked. Either the linker names its output differently now, or the batch produced no binary."
        )
    }
    val exportedNames = readWasmExportNames(wasmFile)
    if ("runBoxTest" in exportedNames) {
        testInfraError(
            "The linked binary of a driver-linked grouped batch exports `runBoxTest` from a per-test " +
                    "`wasiBoxTestRun.kt` helper (${wasmFile.absolutePath}). Helper exports are expected to never reach a " +
                    "grouped link, and the standalone WASI VMs invoke the bare `startTest` export — which is now " +
                    "ambiguous between the driver and the helper, so the batch may run a single `box()` instead of " +
                    "the result-collecting driver."
        )
    }
    if ("startTest" !in exportedNames) {
        testInfraError(
            "The linked binary of a driver-linked grouped batch does not export `startTest` " +
                    "(${wasmFile.absolutePath}), so no WASI VM can invoke the result-collecting driver."
        )
    }
}

/**
 * Rejects unit-test runs that would invoke the per-test `box()` helper on standalone WASI VMs instead of the unit-test runner,
 * because the artifact has no grouped-tests driver to provide the correct entry point.
 */
internal fun checkUnitTestRunnerSupport(
    hasGroupedTestsDriver: Boolean,
    runUnitTests: Boolean,
    vmsToCheck: List<WasmVmDescriptor>,
) {
    if (hasGroupedTestsDriver || !runUnitTests) return

    val standaloneVms = vmsToCheck.filter { !it.entryPointIsJsFile }
    if (standaloneVms.isEmpty()) return

    testInfraError(
        "A `// RUN_UNIT_TESTS` WASI test cannot report its results on ${standaloneVms.map { it.vmName }}: " +
                "those VMs invoke the bare `startTest` export, which is `wasiBoxTestRun.kt`'s `box()` " +
                "helper rather than the unit-test runner. Run such a test on Node.js only, or export a " +
                "unit-test entry point for the standalone VMs to invoke."
    )
}

// TODO reduce amount of duplicated code between this class and WasmBoxRunner
class WasiBoxRunner(
    testServices: TestServices,
    executeWithNodeJsOnly: Boolean = false, // Klib backward compatibility testsuite needs only one best Wasi runner
) : AbstractWasmArtifactsCollector(testServices) {
    internal val vmsToCheck: List<WasmVM> = if (executeWithNodeJsOnly) {
        listOf(WasmVM.NodeJs)
    } else {
        listOf(WasmVM.NodeJs, WasmVM.WasmEdge, WasmVM.Wasmtime)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            runWasmCode(modulesToArtifact.values.single() as WasmCompilationSetsBinaryArtifact)
        }
    }

    /**
     * Runs the WASI compilation set on the WASI VMs (NodeJs / WasmEdge / Wasmtime).
     *
     * Mirrors [WasmBoxRunner.runWasmCode] so that the IN_PROCESS grouping stage
     * ([WasmCompilationSetsGroupingStageBoxRunner]) can delegate WASI runs here instead of to the
     * JS-only [WasmBoxRunner] (whose V8/SpiderMonkey/JSC engines cannot resolve the WASI `wasi`
     * import emitted into `index.mjs`).
     */
    fun runWasmCode(
        artifacts: WasmCompilationSetsBinaryArtifact,
        useUnitTestRunnerOnly: Boolean = false,
        outputCollector: MutableList<WasmVMOutput>? = null,
        throwOnExceptions: Boolean = !useUnitTestRunnerOnly,
    ): List<Throwable> {
        val outputDirBase = testServices.getWasmTestOutputDirectory()

        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val startUnitTests = useUnitTestRunnerOnly || RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives
        val callGroupedTestsDriver = artifacts.hasGroupedTestsDriver

        // Unconditional, as in `WasmWasiFolderGroupingStageBoxRunner`: what makes the unit tests unreachable is the
        // directive plus a driverless standalone VM, not the mode this runner was invoked in. A box run that also
        // starts the unit tests reaches those VMs through the very same bare `startTest` export.
        checkUnitTestRunnerSupport(
            hasGroupedTestsDriver = callGroupedTestsDriver,
            runUnitTests = startUnitTests,
            vmsToCheck = vmsToCheck,
        )

        val testWasiQuiet = if (useUnitTestRunnerOnly) startUnitTestsWasiScript(callGroupedTestsDriver)
        else """
            let boxTestPassed = false;
            try {
                let jsModule = await import('./$WASM_BASE_FILE_NAME.mjs');
                ${if (startUnitTests) "jsModule.startUnitTests();" else ""}
                boxTestPassed = jsModule.runBoxTest();
            } catch(e) {
                console.log('Failed with exception!');
                console.log(e);
            }

            if (!boxTestPassed)
                process.exit(1);
            """.trimIndent()

        val testWasiVerbose = testWasiQuiet + """
            
            
                    console.log('test passed');
                """.trimIndent()

        val testWasi = if (debugMode >= DebugMode.DEBUG) testWasiVerbose else testWasiQuiet

        fun writeToFilesAndRunTest(mode: String, res: WasmCompilerResult): List<Throwable> {
            val dir = File(outputDirBase, mode)
            dir.mkdirs()

            res.writeTo(dir, WASM_BASE_FILE_NAME, debugMode)

            if (callGroupedTestsDriver) assertDriverOwnsStartTestExport(dir)

            File(dir, "test.mjs").writeText(testWasi)
            val collectedJsArtifacts = collectJsArtifacts(originalFile, mode)
            val (jsFilePaths) = collectedJsArtifacts.saveJsArtifacts(dir)
            if (debugMode >= DebugMode.DEBUG) {
                println(" ------ $mode Test file://${dir.absolutePath}/test.mjs")
            }

            val testFileText = originalFile.readText()
            val useNewExceptionProposal = testServices.useNewExceptionHandling(WasmTarget.WASI)

            val exceptions = vmsToCheck.mapNotNull { vm ->
                vm.runWithCaughtExceptions(
                    debugMode = debugMode,
                    useNewExceptionHandling = useNewExceptionProposal,
                    useStackSwitching = false,
                    entryFile = if (!vm.entryPointIsJsFile) "$WASM_BASE_FILE_NAME.wasm" else collectedJsArtifacts.entryPath ?: "test.mjs",
                    jsFilePaths = jsFilePaths,
                    workingDirectory = dir,
                    executionName = formatWasmExecutionName(vm.vmName, mode),
                    outputCollector = outputCollector,
                )
            }

            // TODO KT-71504: support size tests for WASI target and ignoring utility files
            val filesToIgnoreInSizeChecks = emptySet<File>()
            when (mode) {
                "dce" -> checkExpectedDceOutputSize(debugMode, testFileText, dir, filesToIgnoreInSizeChecks)
                "optimized" -> checkExpectedOptimizedOutputSize(debugMode, testFileText, dir, filesToIgnoreInSizeChecks)
            }
            return exceptions
        }

        val allExceptions = mutableListOf<Throwable>()
        allExceptions += writeToFilesAndRunTest("dev", artifacts.compilation.compilerResult)
        artifacts.dceCompilation?.let {
            allExceptions += writeToFilesAndRunTest("dce", it.compilerResult)
        }
        artifacts.optimisedCompilation?.let {
            allExceptions += writeToFilesAndRunTest("optimized", it.compilerResult)
        }

        if (throwOnExceptions) {
            processExceptions(allExceptions)
        }

        return allExceptions
    }
}

/*
 * This Wasi folder runner is intended for the future use in "WasmWasi Klib forward compatibility tests", should it be ever needed,
 * similar to [CustomWasmJsCompilerSecondStageTestGenerated]
 */
open class WasmWasiFolderGroupingStageBoxRunner(
    testServices: TestServices
) : AbstractWasmGroupingStageBoxRunner(testServices), WasmArtifactsCollector {
    private val firstNonGroupingTestServices: TestServices
        get() = testServices.groupingStageInputs.first().testServices
    private val vmsToCheck: List<WasmVM> = listOf(WasmVM.NodeJs, WasmVM.WasmEdge, WasmVM.Wasmtime)

    override fun shouldUseBoxExportMode(): Boolean {
        // WASI tests always use the unit-test runner, never box export mode
        return false
    }

    override fun runTestCode(
        artifact: BinaryArtifacts.Wasm,
        useUnitTestRunnerOnly: Boolean,
        outputCollector: MutableList<WasmVMOutput>?,
    ): List<Throwable> {
        val folderArtifact = artifact as WasmFolderBinaryArtifact
        val folder = folderArtifact.folder
        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")

        checkUnitTestRunnerSupport(
            hasGroupedTestsDriver = folderArtifact.hasGroupedTestsDriver,
            runUnitTests = useUnitTestRunnerOnly ||
                    RUN_UNIT_TESTS in firstNonGroupingTestServices.moduleStructure.allDirectives,
            vmsToCheck = vmsToCheck,
        )

        val callGroupedTestsDriver = folderArtifact.hasGroupedTestsDriver
        if (callGroupedTestsDriver) assertDriverOwnsStartTestExport(folder)

        val testWasi = startUnitTestsWasiScript(callGroupedTestsDriver)
        File(folder, "test.mjs").writeText(testWasi)

        val collectedOutputs = outputCollector ?: mutableListOf()
        return vmsToCheck.mapNotNull { vm ->
            vm.runWithCaughtExceptions(
                debugMode = debugMode,
                firstNonGroupingTestServices.useNewExceptionHandling(WasmTarget.WASI),
                useStackSwitching = false,
                entryFile = if (!vm.entryPointIsJsFile) "$WASM_BASE_FILE_NAME.wasm" else "test.mjs",
                jsFilePaths = emptyList(),
                workingDirectory = folder,
                executionName = formatWasmExecutionName(vm.vmName, "dev"),
                outputCollector = collectedOutputs,
            )
        }
    }
}

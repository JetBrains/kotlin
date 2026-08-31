/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.TestInfrastructureException
import org.jetbrains.kotlin.wasm.test.blackbox.WasmWasiGroupedTestsExportedEntryPointGenerator
import org.jetbrains.kotlin.wasm.test.tools.WasmVmDescriptor
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

/**
 * Pins what the WASI `test.mjs` launcher calls: `startTest()` is both the grouped driver and — via
 * `wasiBoxTestRun.kt` — the `box()` helper of every box test, so the export names cannot tell the two apart.
 */
class WasiLauncherScriptTest {
    @Test
    fun `given a batch with the generated driver then the launcher calls it`() {
        val script = startUnitTestsWasiScript(callGroupedTestsDriver = true)

        assertTrue("jsModule.startTest();" in script, script)
        // The driver reports through stdout, so the unit-test runner must not be driven on top of it.
        assertFalse("startUnitTests" in script, script)
    }

    @Test
    fun `given the generated WASI entry point then the launcher calls exactly the name it exports`() {
        val entryPoint = WasmWasiGroupedTestsExportedEntryPointGenerator.generateExportedEntryPointSource("__runAll")

        // `startTest` is also what the standalone VMs invoke directly, so the name is part of two contracts at once.
        assertTrue("@kotlin.wasm.WasmExport" in entryPoint, entryPoint)
        assertTrue("__runAll()" in entryPoint, entryPoint)

        val exportedName = EXPORTED_FUNCTION_NAME.find(entryPoint)?.groupValues?.get(1)
        assertEquals("startTest", exportedName, entryPoint)
        assertTrue(
            "jsModule.$exportedName();" in startUnitTestsWasiScript(callGroupedTestsDriver = true),
            entryPoint,
        )
    }

    @Test
    fun `given a batch without the driver then the launcher calls the unit-test runner`() {
        // The case an export-name probe got wrong: calling `startTest` here would run `box()` instead of the unit tests.
        val script = startUnitTestsWasiScript(callGroupedTestsDriver = false)

        assertTrue("jsModule.startUnitTests();" in script, script)
        assertFalse("startTest" in script, script)
    }

    @Test
    fun `given a driverless unit-test run on a standalone VM then the run is rejected`() {
        // Those VMs invoke the bare `startTest` export, which without a driver is `wasiBoxTestRun.kt`'s `box()`
        // helper: the unit tests would not run at all and the batch would still be green.
        val error = assertThrows(TestInfrastructureException::class.java) {
            checkUnitTestRunnerSupport(
                hasGroupedTestsDriver = false,
                runUnitTests = true,
                vmsToCheck = listOf(nodeJs, wasmEdge, wasmtime),
            )
        }

        val message = error.message.orEmpty()
        assertTrue(wasmEdge.vmName in message, message)
        assertTrue(wasmtime.vmName in message, message)
        assertFalse(nodeJs.vmName in message, message)
    }

    @Test
    fun `given a driver or a JS-entry-point VM then the unit-test run is accepted`() {
        // With the driver linked in, `startTest` is the driver itself; Node.js goes through `test.mjs` either way.
        assertDoesNotThrow {
            checkUnitTestRunnerSupport(
                hasGroupedTestsDriver = true,
                runUnitTests = true,
                vmsToCheck = listOf(wasmEdge, wasmtime),
            )
        }
        assertDoesNotThrow {
            checkUnitTestRunnerSupport(
                hasGroupedTestsDriver = false,
                runUnitTests = true,
                vmsToCheck = listOf(nodeJs),
            )
        }
        assertDoesNotThrow {
            checkUnitTestRunnerSupport(
                hasGroupedTestsDriver = false,
                runUnitTests = false,
                vmsToCheck = listOf(wasmEdge),
            )
        }
    }

    @Test
    fun `given an explicit unit-test-only invocation on a standalone VM then it is rejected`() {
        val error = assertThrows(TestInfrastructureException::class.java) {
            checkUnitTestRunnerSupport(
                hasGroupedTestsDriver = false,
                runUnitTests = true,
                vmsToCheck = listOf(wasmtime),
            )
        }

        assertTrue("unit-test runner" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `given a grouped binary exporting only the driver then the export-surface check passes`(@TempDir dir: File) {
        // The JavaScript wrapper may mention these names for reasons unrelated to the Wasm export surface.
        dir.resolve("index.mjs").writeText("const helperName = 'runBoxTest'\n")
        writeWasmModule(dir, "startTest", "startUnitTests")

        assertDoesNotThrow { assertDriverOwnsStartTestExport(dir) }

        // No glue at all is not this check's business (e.g. a mode that produced none).
        assertDoesNotThrow { assertDriverOwnsStartTestExport(dir.resolve("no-such-subdir")) }
    }

    @Test
    fun `given a helper export leaking into a grouped binary then the export-surface check fails`(@TempDir dir: File) {
        dir.resolve("index.mjs").writeText("export const { startTest } = exports\n")
        writeWasmModule(dir, "runBoxTest", "startTest")

        val error = assertThrows(TestInfrastructureException::class.java) { assertDriverOwnsStartTestExport(dir) }
        assertTrue("runBoxTest" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `given a grouped binary without a startTest export then the export-surface check fails`(@TempDir dir: File) {
        dir.resolve("index.mjs").writeText("export const { startTest } = exports\n")
        writeWasmModule(dir, "startUnitTests")

        val error = assertThrows(TestInfrastructureException::class.java) { assertDriverOwnsStartTestExport(dir) }
        assertTrue("does not export `startTest`" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `given a non-function export named startTest then the export-surface check rejects it`(@TempDir dir: File) {
        dir.resolve("index.mjs").writeText("export const { startTest } = exports\n")
        dir.resolve("index.wasm").writeBytes(
            wasmModuleBytes(
                exports = listOf("startTest"),
                exportKinds = mapOf("startTest" to 2), // memory export
            )
        )

        val error = assertThrows(TestInfrastructureException::class.java) { assertDriverOwnsStartTestExport(dir) }
        assertTrue("does not export `startTest`" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `given sections whose sizes need multi-byte lengths then the exports are still found`(@TempDir dir: File) {
        // The size of every section here needs two LEB128 bytes, as it does in any real linked binary.
        val longExportName = "padding_export_" + "x".repeat(200)
        dir.resolve("index.wasm").writeBytes(
            wasmModuleBytes(
                exports = listOf(longExportName, "startTest"),
                extraSections = listOf(0 to ByteArray(300) { 0x2A }),
            )
        )

        assertDoesNotThrow { assertDriverOwnsStartTestExport(dir) }
    }

    @Test
    fun `given a binary truncated inside a section then the export-surface check reports it as unreadable`(
        @TempDir dir: File,
    ) {
        // Seeking over a section is allowed to run past the end of the file, which would look like "no exports at all".
        val bytes = wasmModuleBytes(exports = listOf("startTest"), extraSections = listOf(0 to ByteArray(300)))
        dir.resolve("index.wasm").writeBytes(bytes.copyOf(bytes.size - 200))

        val error = assertThrows(TestInfrastructureException::class.java) { assertDriverOwnsStartTestExport(dir) }
        assertTrue("Failed to read Wasm exports" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `given an output folder without the linked binary then the export-surface check fails`(@TempDir dir: File) {
        // The check must not turn into a no-op just because the binary is not where it is expected to be.
        dir.resolve("index.mjs").writeText("export const { startTest } = exports\n")

        val error = assertThrows(TestInfrastructureException::class.java) { assertDriverOwnsStartTestExport(dir) }
        assertTrue("no `index.wasm`" in error.message.orEmpty(), error.message.orEmpty())
    }

    private companion object {
        /** The name the VM invokes: whatever the generator declares is what the launcher script has to call. */
        val EXPORTED_FUNCTION_NAME = Regex("""fun\s+(\w+)\(""")

        /** `\0asm` followed by binary format version 1, little-endian — the eight bytes every module starts with. */
        val WASM_HEADER = byteArrayOf(0, 0x61, 0x73, 0x6D, 1, 0, 0, 0)
    }

    @Test
    fun `given an export section carrying more entries than it declares then the binary is rejected`(@TempDir dir: File) {
        // Declares one export but holds the bytes of two: a section that does not match its own framing cannot be
        // trusted to have been read to the end, so the exports it did yield say nothing about the real surface.
        val exportSection = ByteArrayOutputStream().apply {
            writeUleb128(1)
            listOf("startTest", "leftoverExport").forEach { export ->
                val name = export.toByteArray()
                writeUleb128(name.size)
                write(name)
                write(0) // function export
                write(0) // export index
            }
        }.toByteArray()

        dir.resolve("index.wasm").writeBytes(
            ByteArrayOutputStream().apply {
                write(WASM_HEADER)
                writeSection(7, exportSection)
            }.toByteArray()
        )

        val error = assertThrows(TestInfrastructureException::class.java) { assertDriverOwnsStartTestExport(dir) }
        assertTrue("trailing bytes" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `given a section size that overflows 32 bits then the binary is rejected`(@TempDir dir: File) {
        // A five-byte LEB128 whose last byte carries bits past the 32nd — no real section size looks like this.
        dir.resolve("index.wasm").writeBytes(
            ByteArrayOutputStream().apply {
                write(WASM_HEADER)
                write(0) // custom section
                write(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x10))
            }.toByteArray()
        )

        val error = assertThrows(TestInfrastructureException::class.java) { assertDriverOwnsStartTestExport(dir) }
        assertTrue("Invalid unsigned 32-bit LEB128 number" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `given an export section larger than the file then it is rejected before anything is allocated`(
        @TempDir dir: File,
    ) {
        // A section size is what the sub-reader takes as its budget, and a name length inside it is bounded by that
        // budget alone — so a corrupt pair of sizes used to reach `ByteArray(size.toInt())` and allocate on the
        // strength of a number no byte of the file backs. At the sizes a garbled binary can declare (just under
        // 2 GB) that allocation is an `OutOfMemoryError`, which, being an `Error`, escapes the reader's `catch` and
        // kills the test JVM rather than reporting the binary as unreadable. Both sizes here overrun the file, so
        // the failure has to name that overrun and not merely the end of the stream it would otherwise run into.
        val declaredSectionSize = 64 * 1024 * 1024
        val declaredNameLength = declaredSectionSize - 8

        dir.resolve("index.wasm").writeBytes(
            ByteArrayOutputStream().apply {
                write(WASM_HEADER)
                write(7) // export section
                writeUleb128(declaredSectionSize)
                writeUleb128(1) // one export announced
                writeUleb128(declaredNameLength)
            }.toByteArray()
        )

        val error = assertThrows(TestInfrastructureException::class.java) { assertDriverOwnsStartTestExport(dir) }
        val message = error.message.orEmpty()
        assertTrue("Failed to read Wasm exports" in message, message)
        assertTrue("declares more bytes" in message, message)
    }

    @Test
    fun `given an oversized export name backed by a sparse file then inspection rejects it before allocating`(
        @TempDir dir: File,
    ) {
        val declaredNameLength = MAX_WASM_EXPORT_NAME_SIZE + 1
        val sectionPrefix = ByteArrayOutputStream().apply {
            writeUleb128(1) // one unrelated export announced
            writeUleb128(declaredNameLength.toInt())
        }.toByteArray()
        val declaredSectionSize = sectionPrefix.size + declaredNameLength + 2
        val prefix = ByteArrayOutputStream().apply {
            write(WASM_HEADER)
            write(7) // export section
            writeUleb128(declaredSectionSize.toInt())
            write(sectionPrefix)
        }.toByteArray()
        val wasmFile = dir.resolve("index.wasm").also { it.writeBytes(prefix) }

        // Back the declared name with the file without materializing its bytes. The parser must skip the unrelated
        // name before it attempts to allocate or read the hole, then report the genuinely missing required export.
        RandomAccessFile(wasmFile, "rw").use { it.setLength(prefix.size.toLong() + declaredSectionSize) }

        val error = assertThrows(TestInfrastructureException::class.java) { assertDriverOwnsStartTestExport(dir) }
        val message = error.message.orEmpty()
        assertTrue("does not export `startTest`" in message, message)
    }

    @Test
    fun `given both required exports before an oversized unrelated export then inspection stops looking for names`(
        @TempDir dir: File,
    ) {
        val declaredSectionSize = MAX_WASM_EXPORT_NAME_SIZE + 64
        val declaredUnrelatedNameLength = MAX_WASM_EXPORT_NAME_SIZE + 1
        val prefix = ByteArrayOutputStream().apply {
            write(WASM_HEADER)
            write(7) // export section
            writeUleb128(declaredSectionSize.toInt())
            writeUleb128(3) // startTest, runBoxTest, and one unrelated export
            writeExport("startTest")
            writeExport("runBoxTest")
            writeUleb128(declaredUnrelatedNameLength.toInt())
        }.toByteArray()
        val wasmFile = dir.resolve("index.wasm").also { it.writeBytes(prefix) }

        // The unrelated name is backed by a sparse file. The targeted inspector must skip it rather than decode and
        // reject the name-length field after it already found both exports it needs.
        RandomAccessFile(wasmFile, "rw").use { it.setLength(prefix.size.toLong() + declaredSectionSize) }

        assertEquals(setOf("startTest", "runBoxTest"), readWasmExportNames(wasmFile))
    }

    @Test
    fun `given startTest before an oversized unrelated export then inspection skips the unrelated name`(@TempDir dir: File) {
        val declaredUnrelatedNameLength = MAX_WASM_EXPORT_NAME_SIZE + 1
        val exportSectionPrefix = ByteArrayOutputStream().apply {
            writeUleb128(2) // startTest and one unrelated export
            writeExport("startTest")
            writeUleb128(declaredUnrelatedNameLength.toInt())
        }.toByteArray()
        val declaredSectionSize = exportSectionPrefix.size + declaredUnrelatedNameLength + 2
        val modulePrefix = ByteArrayOutputStream().apply {
            write(WASM_HEADER)
            write(7) // export section
            writeUleb128(declaredSectionSize.toInt())
            write(exportSectionPrefix)
        }.toByteArray()
        val wasmFile = dir.resolve("index.wasm").also { it.writeBytes(modulePrefix) }

        // The unrelated export name is sparse-file backed. Reading it into a ByteArray would be an unnecessary
        // allocation, while skipping it still validates that its declared bytes and the enclosing section exist.
        RandomAccessFile(wasmFile, "rw").use { it.setLength(modulePrefix.size.toLong() + declaredSectionSize) }

        assertEquals(setOf("startTest"), readWasmExportNames(wasmFile))
    }

    @Test
    fun `given an export section beyond the inspection budget then it is rejected before reading the section`(
        @TempDir dir: File,
    ) {
        val declaredSectionSize = MAX_WASM_EXPORT_SECTION_SIZE + 1
        dir.resolve("index.wasm").writeBytes(
            ByteArrayOutputStream().apply {
                write(WASM_HEADER)
                write(7) // export section
                writeUleb128(declaredSectionSize.toInt())
            }.toByteArray()
        )

        val error = assertThrows(TestInfrastructureException::class.java) { assertDriverOwnsStartTestExport(dir) }
        val message = error.message.orEmpty()
        assertTrue("export section is too large" in message, message)
    }

    private fun writeWasmModule(dir: File, vararg exports: String) {
        dir.resolve("index.wasm").writeBytes(wasmModuleBytes(exports.toList()))
    }

    /** Stands in for [org.jetbrains.kotlin.wasm.test.tools.WasmVM.NodeJs] without resolving a real engine path. */
    private val nodeJs = fakeVm("NodeJs", entryPointIsJsFile = true)

    /** Stands in for [org.jetbrains.kotlin.wasm.test.tools.WasmVM.WasmEdge]. */
    private val wasmEdge = fakeVm("WasmEdge", entryPointIsJsFile = false)

    /** Stands in for [org.jetbrains.kotlin.wasm.test.tools.WasmVM.Wasmtime]. */
    private val wasmtime = fakeVm("Wasmtime", entryPointIsJsFile = false)

    private fun fakeVm(name: String, entryPointIsJsFile: Boolean): WasmVmDescriptor =
        object : WasmVmDescriptor {
            override val vmName: String = name
            override val entryPointIsJsFile: Boolean = entryPointIsJsFile
        }

    /** [extraSections] are written before the export section, so the reader has to skip over them to reach it. */
    private fun wasmModuleBytes(
        exports: List<String>,
        extraSections: List<Pair<Int, ByteArray>> = emptyList(),
        exportKinds: Map<String, Int> = emptyMap(),
    ): ByteArray {
        val exportSection = ByteArrayOutputStream().apply {
            writeUleb128(exports.size)
            exports.forEach { export ->
                val name = export.toByteArray()
                writeUleb128(name.size)
                write(name)
                write(exportKinds[export] ?: 0)
                write(0) // function index
            }
        }.toByteArray()

        return ByteArrayOutputStream().apply {
            write(WASM_HEADER)
            // Include a GC type that the full Wasm-to-IR parser does not support. Export inspection must not parse it.
            writeSection(1, byteArrayOf(2, 0x60, 0, 0, 0x5F, 0))
            writeSection(3, byteArrayOf(1, 0))
            extraSections.forEach { [id, payload] -> writeSection(id, payload) }
            writeSection(7, exportSection)
            writeSection(10, byteArrayOf(1, 2, 0, 0x0B))
        }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeSection(id: Int, payload: ByteArray) {
        write(id)
        writeUleb128(payload.size)
        write(payload)
    }

    private fun ByteArrayOutputStream.writeExport(name: String) {
        val bytes = name.toByteArray()
        writeUleb128(bytes.size)
        write(bytes)
        write(0) // function export
        write(0) // export index
    }

    /** Section sizes and name lengths are LEB128 on the wire, and a real `index.wasm` never fits them into one byte. */
    private fun ByteArrayOutputStream.writeUleb128(value: Int) {
        var rest = value
        do {
            val byte = rest and 0x7F
            rest = rest ushr 7
            write(if (rest == 0) byte else byte or 0x80)
        } while (rest != 0)
    }
}

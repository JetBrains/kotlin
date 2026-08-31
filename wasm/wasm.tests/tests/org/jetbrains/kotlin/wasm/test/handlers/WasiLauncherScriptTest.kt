/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.TestInfrastructureException
import org.jetbrains.kotlin.wasm.test.blackbox.WasmWasiGroupedTestsExportedEntryPointGenerator
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

class WasiLauncherScriptTest {
    @Test
    fun `given a batch with the generated driver then the launcher calls it`() {
        val script = startUnitTestsWasiScript(callGroupedTestsDriver = true)

        assertTrue("jsModule.startTest();" in script, script)
        assertFalse("startUnitTests" in script, script)
    }

    @Test
    fun `given the generated WASI entry point then the launcher calls exactly the name it exports`() {
        val entryPoint = WasmWasiGroupedTestsExportedEntryPointGenerator.generateExportedEntryPointSource("__runAll")

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
        val script = startUnitTestsWasiScript(callGroupedTestsDriver = false)

        assertTrue("jsModule.startUnitTests();" in script, script)
        assertFalse("startTest" in script, script)
    }

    @Test
    fun `given a grouped binary exporting only the driver then the export-surface check passes`(@TempDir dir: File) {
        dir.resolve("index.mjs").writeText("const helperName = 'runBoxTest'\n")
        writeWasmModule(dir, "startTest", "startUnitTests")

        assertDoesNotThrow { assertDriverOwnsStartTestExport(dir) }

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
        val bytes = wasmModuleBytes(exports = listOf("startTest"), extraSections = listOf(0 to ByteArray(300)))
        dir.resolve("index.wasm").writeBytes(bytes.copyOf(bytes.size - 200))

        val error = assertThrows(TestInfrastructureException::class.java) { assertDriverOwnsStartTestExport(dir) }
        assertTrue("Failed to read Wasm exports" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `given an output folder without the linked binary then the export-surface check fails`(@TempDir dir: File) {
        dir.resolve("index.mjs").writeText("export const { startTest } = exports\n")

        val error = assertThrows(TestInfrastructureException::class.java) { assertDriverOwnsStartTestExport(dir) }
        assertTrue("no `index.wasm`" in error.message.orEmpty(), error.message.orEmpty())
    }

    private companion object {
        val EXPORTED_FUNCTION_NAME = Regex("""fun\s+(\w+)\(""")

        val WASM_HEADER = byteArrayOf(0, 0x61, 0x73, 0x6D, 1, 0, 0, 0)
    }

    @Test
    fun `given an export section carrying more entries than it declares then the binary is rejected`(@TempDir dir: File) {
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

    private fun ByteArrayOutputStream.writeUleb128(value: Int) {
        var rest = value
        do {
            val byte = rest and 0x7F
            rest = rest ushr 7
            write(if (rest == 0) byte else byte or 0x80)
        } while (rest != 0)
    }
}

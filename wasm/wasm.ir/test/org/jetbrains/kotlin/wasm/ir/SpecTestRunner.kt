/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.test.KotlinTestUtils.assertEqualsToFile
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import org.jetbrains.kotlin.wasm.ir.convertors.MyByteReader
import org.jetbrains.kotlin.wasm.ir.convertors.WasmBinaryBuilder
import org.jetbrains.kotlin.wasm.ir.convertors.WasmBinaryToIR
import org.jetbrains.kotlin.wasm.ir.convertors.WatBuilder
import java.io.ByteArrayOutputStream
import java.io.File

@Suppress("unused")
@Serializable
data class SpecTest(
    val source_filename: String,
    val commands: List<Command>
) {
    @Serializable
    sealed class Command {
        @SerialName("module")
        @Serializable
        data class Module(
            val line: Int,
            val filename: String,
            val name: String? = null,
        ) : Command()

        @SerialName("register")
        @Serializable
        data class Register(
            val line: Int,
            val name: String? = null,
            val `as`: String? = null
        ) : Command()

        @SerialName("assert_return")
        @Serializable
        data class AssertReturn(
            val line: Int,
            val action: Action,
            val expected: List<Value>,
        ) : Command()

        // TODO: Assert trap for modules?
        @SerialName("assert_trap")
        @Serializable
        data class AssertTrap(
            val line: Int,
            val action: Action,
            val text: String,
            val expected: List<Value>,
        ) : Command()

        @SerialName("assert_exhaustion")
        @Serializable
        data class AssertExhaustion(
            val line: Int,
            val action: Action,
            val text: String,
            val expected: List<Value>,
        ) : Command()

        @SerialName("assert_malformed")
        @Serializable
        data class AssertMalformed(
            val line: Int,
            val filename: String,
            val text: String,
            val module_type: String,
        ) : Command()

        @SerialName("assert_invalid")
        @Serializable
        data class AssertInvalid(
            val line: Int,
            val filename: String,
            val text: String,
            val module_type: String,
        ) : Command()

        @SerialName("assert_unlinkable")
        @Serializable
        data class AssertUnlinkable(
            val line: Int,
            val filename: String,
            val text: String,
            val module_type: String,
        ) : Command()

        @SerialName("assert_uninstantiable")
        @Serializable
        data class AssertUninstantiable(
            val line: Int,
            val filename: String,
            val text: String,
            val module_type: String,
        ) : Command()

        @SerialName("action")
        @Serializable
        data class ActionCommand(
            val line: Int,
            val action: Action,
            val expected: List<Value>,
        ) : Command()
    }

    @Serializable
    data class Action(
        val type: String,
        val field: String,
        val args: List<Value> = emptyList(),
        val module: String? = null
    )

    @Serializable
    data class Value(
        val type: String,
        val value: String? = null
    )
}

private fun runSpecTest(specTest: SpecTest, testDir: File, wastFile: File, wabtOptions: List<String>) {
    for (command in specTest.commands) {
        when (command) {
            is SpecTest.Command.Module -> {
                val wasmFile = File(testDir, command.filename)
                testWasmFile(wasmFile, testDir.name)
            }
        }
    }
}

private fun runJsonTest(jsonFile: File, wastFile: File, wabtOptions: List<String>) {
    require(jsonFile.isFile && jsonFile.exists())
    val jsonText = jsonFile.readText()
    val specTest = Json.decodeFromString(SpecTest.serializer(), jsonText)
    val wasmDir = jsonFile.parentFile!!
    println("Running json test ${jsonFile.path} ...")
    runSpecTest(specTest, wasmDir, wastFile, wabtOptions)
}

val wasmTestSuitePath: String
    get() = System.getProperty("wasm.testsuite.path")!!

fun testProposal(
    name: String,
    wabtOptions: List<String> = listOf("--enable-all"),
    ignoreFiles: List<String> = emptyList()
) {

    runSpecTests(name, "$wasmTestSuitePath/proposals/$name", wabtOptions, ignoreFiles)
}


fun runSpecTests(
    name: String,
    wastDirectoryPath: String,
    wabtOptions: List<String>,
    ignoreFiles: List<String> = emptyList()
) {
    // Clean and prepare output dir for spec tests
    val specTestsDir = File("build/spec-tests/$name")
    if (specTestsDir.exists())
        specTestsDir.deleteRecursively()
    specTestsDir.mkdirs()

    val testSuiteDir = File(wastDirectoryPath)
    assert(testSuiteDir.isDirectory) { "${testSuiteDir.absolutePath} is not a directory" }
    for (file in testSuiteDir.listFiles()!!) {
        if (file.name in ignoreFiles) {
            println("Ignoring file: ${file.absolutePath}")
            continue
        }
        if (file.isFile && file.name.endsWith(".wast")) {
            val jsonFileName = file.withReplacedExtensionOrNull(".wast", ".json")!!.name
            val jsonFile = File(specTestsDir, jsonFileName)
            println("Creating JSON for ${file.path}")
            Wabt.wast2json(file, jsonFile, *wabtOptions.toTypedArray())
            runJsonTest(jsonFile, file, wabtOptions)
        }
    }
}


fun testWasmFile(wasmFile: File, dirName: String) {
    val testName = wasmFile.nameWithoutExtension

    fun newFile(suffix: String): File =
        File("build/spec-tests/tmp/$dirName/${testName}_$suffix")
            .also {
                it.parentFile.mkdirs()
                it.createNewFile()
            }

    println("Testing wasm file : ${wasmFile.absolutePath} ... ")
    val module = fileToWasmModule(wasmFile)
    val kotlinTextFormat = module.toTextFormat()
    val kotlinBinaryFormat = module.toBinaryFormat()

    val kotlinTextFile = newFile("kwt.wat")
    kotlinTextFile.writeText(kotlinTextFormat)
    val kotlinBinaryFile = newFile("kwt.wasm")
    kotlinBinaryFile.writeBytes(kotlinBinaryFormat)

    val kotlinTextToWasmTmpFile = newFile("kwt.tmp.wasm")
    Wabt.wat2wasm(kotlinTextFile, kotlinTextToWasmTmpFile)

    val kotlinTextCanonicalFile = newFile("kwt.canonical.wat")
    Wabt.wasm2wat(kotlinTextToWasmTmpFile, kotlinTextCanonicalFile)

    val wabtWatFile = newFile("wabt.wat")
    Wabt.wasm2wat(wasmFile, wabtWatFile)

    assertEqualsToFile("Kwt text format", wabtWatFile, kotlinTextCanonicalFile.readText())

    val kotlinBinaryCanonicalFile = newFile("kwt.bin.canonical.wat")
    Wabt.wasm2wat(kotlinBinaryFile, kotlinBinaryCanonicalFile)
    assertEqualsToFile("Kwt binary format", wabtWatFile, kotlinBinaryCanonicalFile.readText())
}

fun WasmModule.toBinaryFormat(): ByteArray {
    val os = ByteArrayOutputStream()
    WasmBinaryBuilder(os, this).appendWasmModule()
    return os.toByteArray()
}

fun WasmModule.toTextFormat(): String {
    val builder = WatBuilder()
    builder.appendWasmModule(this)
    return builder.toString()
}

fun fileToWasmModule(file: File): WasmModule =
    WasmBinaryToIR(MyByteReader(file.inputStream())).parseModule()

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import java.io.File

class WasmBoxRunner(
    testServices: TestServices
) : AbstractWasmArtifactsCollector(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            runWasmCode()
        }
    }

    private fun runWasmCode() {
        val artifacts = modulesToArtifact.values.single()
        val baseFileName = "index"
        val outputDirBase = testServices.getWasmTestOutputDirectory()

        val jsFiles = mutableListOf<AdditionalFile>()
        val mjsFiles = mutableListOf<AdditionalFile>()
        var entryMjs: String? = "test.mjs"

        testServices.moduleStructure.modules.forEach { m ->
            m.files.forEach { file: TestFile ->
                val name = file.name
                when {
                    name.endsWith(".js") ->
                        jsFiles += AdditionalFile(file.name, file.originalContent)

                    name.endsWith(".mjs") -> {
                        mjsFiles += AdditionalFile(file.name, file.originalContent)
                        if (name == "entry.mjs") {
                            entryMjs = name
                        }
                    }
                }
            }
        }

        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

        originalFile.parentFile.resolve(originalFile.nameWithoutExtension + JavaScript.DOT_EXTENSION)
            .takeIf { it.exists() }
            ?.let {
                jsFiles += AdditionalFile(it.name, it.readText())
            }

        originalFile.parentFile.resolve(originalFile.nameWithoutExtension + JavaScript.DOT_MODULE_EXTENSION)
            .takeIf { it.exists() }
            ?.let {
                mjsFiles += AdditionalFile(it.name, it.readText())
            }

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val startUnitTests = RUN_UNIT_TESTS in testServices.moduleStructure.allDirectives

        val testJsQuiet = """
                    let actualResult;
                    try {
                        // Use "dynamic import" to catch exception happened during JS & Wasm modules initialization
                        let jsModule = await import('./index.mjs');
                        let wasmExports = jsModule.default;
                        ${if (startUnitTests) "wasmExports.startUnitTests();" else ""}
                        actualResult = wasmExports.box();
                    } catch(e) {
                        console.log('Failed with exception!')
                        console.log('Message: ' + e.message)
                        console.log('Name:    ' + e.name)
                        console.log('Stack:')
                        console.log(e.stack)
                    }
    
                    if (actualResult !== "OK")
                        throw `Wrong box result '${'$'}{actualResult}'; Expected "OK"`;
                """.trimIndent()

        val testJsVerbose = testJsQuiet + """
                    console.log('test passed');
                """.trimIndent()

        val testJs = if (debugMode >= DebugMode.DEBUG) testJsVerbose else testJsQuiet

        fun checkExpectedOutputSize(testFileContent: String, testDir: File) {
            val expectedSizes =
                InTextDirectivesUtils.findListWithPrefixes(testFileContent, "// WASM_DCE_EXPECTED_OUTPUT_SIZE: ")
                    .map {
                        val i = it.indexOf(' ')
                        val extension = it.substring(0, i)
                        val size = it.substring(i + 1)
                        extension.trim().lowercase() to size.filter(Char::isDigit).toInt()
                    }

            val filesByExtension = testDir.listFiles()?.groupBy { it.extension }.orEmpty()

            val errors = expectedSizes.mapNotNull { (extension, expectedSize) ->
                val totalSize = filesByExtension[extension].orEmpty().sumOf { it.length() }

                val thresholdPercent = 1
                val thresholdInBytes = expectedSize * thresholdPercent / 100

                val expectedMinSize = expectedSize - thresholdInBytes
                val expectedMaxSize = expectedSize + thresholdInBytes

                val diff = totalSize - expectedSize

                val message = "Total size of $extension files is $totalSize," +
                        " but expected $expectedSize ∓ $thresholdInBytes [$expectedMinSize .. $expectedMaxSize]." +
                        " Diff: $diff (${diff * 100 / expectedSize}%)"

                if (debugMode >= DebugMode.DEBUG) {
                    println(" ------ $message")
                }

                if (totalSize !in expectedMinSize..expectedMaxSize) message else null
            }

            if (errors.isNotEmpty()) throw AssertionError(errors.joinToString("\n"))
        }

        fun writeToFilesAndRunTest(mode: String, res: WasmCompilerResult) {
            val dir = File(outputDirBase, mode)
            dir.mkdirs()

            writeCompilationResult(res, dir, baseFileName)
            File(dir, "test.mjs").writeText(testJs)

            for (mjsFile: AdditionalFile in mjsFiles) {
                File(dir, mjsFile.name).writeText(mjsFile.content)
            }

            val jsFilePaths = mutableListOf<String>()
            for (jsFile: AdditionalFile in jsFiles) {
                val file = File(dir, jsFile.name)
                file.writeText(jsFile.content)
                jsFilePaths += file.canonicalPath
            }

            if (debugMode >= DebugMode.DEBUG) {
                File(dir, "index.html").writeText(
                    """
                                <!DOCTYPE html>
                                <html lang="en">
                                <body>
                                    <span id="test">UNKNOWN</span>
                                    <script type="module">
                                        let test = document.getElementById("test")
                                        try {
                                            await import("./test.mjs");
                                            test.style.backgroundColor = "#0f0";
                                            test.textContent = "OK"
                                        } catch(e) {
                                            test.style.backgroundColor = "#f00";
                                            test.textContent = "NOT OK"
                                            throw e;
                                        }
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                )

                val path = dir.absolutePath
                println(" ------ $mode Wat  file://$path/index.wat")
                println(" ------ $mode Wasm file://$path/index.wasm")
                println(" ------ $mode JS   file://$path/index.uninstantiated.mjs")
                println(" ------ $mode JS   file://$path/index.mjs")
                println(" ------ $mode Test file://$path/test.mjs")
                val projectName = "kotlin"
                println(" ------ $mode HTML http://0.0.0.0:63342/$projectName/${dir.path}/index.html")
                for (mjsFile: AdditionalFile in mjsFiles) {
                    println(" ------ $mode External ESM file://$path/${mjsFile.name}")
                }
            }

            val testFileText = originalFile.readText()
            val failsIn: List<String> = InTextDirectivesUtils.findListWithPrefixes(testFileText, "// WASM_FAILS_IN: ")

            val exceptions = listOf(WasmVM.V8, WasmVM.SpiderMonkey).mapNotNull map@{ vm ->
                try {
                    if (debugMode >= DebugMode.DEBUG) {
                        println(" ------ Run in ${vm.name}" + if (vm.shortName in failsIn) " (expected to fail)" else "")
                    }
                    vm.run(
                        "./${entryMjs}",
                        jsFilePaths,
                        workingDirectory = dir
                    )
                    if (vm.shortName in failsIn) {
                        return@map AssertionError("The test expected to fail in ${vm.name}. Please update the testdata.")
                    }
                } catch (e: Throwable) {
                    if (vm.shortName !in failsIn) {
                        return@map e
                    }
                }
                null
            }

            when (exceptions.size) {
                0 -> {} // Everything OK
                1 -> {
                    throw exceptions.single()
                }
                else -> {
                    throw AssertionError("Failed with several exceptions. Look at suppressed exceptions below.").apply {
                        exceptions.forEach { addSuppressed(it) }
                    }
                }
            }

            if (mode == "dce") {
                checkExpectedOutputSize(testFileText, dir)
            }
        }

        writeToFilesAndRunTest("dev", artifacts.compilerResult)
        writeToFilesAndRunTest("dce", artifacts.compilerResultWithDCE)
    }

    private class AdditionalFile(val name: String, val content: String)
}

fun TestServices.getWasmTestOutputDirectory(): File {
    val originalFile = moduleStructure.originalTestDataFiles.first()
    val allDirectives = moduleStructure.allDirectives

    val pathToRootOutputDir = allDirectives[WasmEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR].first()
    val testGroupDirPrefix = allDirectives[WasmEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX].first()
    val pathToTestDir = allDirectives[WasmEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR].first()

    val testGroupOutputDir = File(File(pathToRootOutputDir, "out"), testGroupDirPrefix)
    val stopFile = File(pathToTestDir)
    return generateSequence(originalFile.parentFile) { it.parentFile }
        .takeWhile { it != stopFile }
        .map { it.name }
        .toList().asReversed()
        .fold(testGroupOutputDir, ::File)
        .let { File(it, originalFile.nameWithoutExtension) }
}
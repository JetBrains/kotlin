/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.compatibility.binary.AbstractKlibBinaryCompatibilityTest
import org.jetbrains.kotlin.compatibility.binary.TestFile
import org.jetbrains.kotlin.compatibility.binary.TestModule
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.MAIN_MODULE_NAME
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.test.Directives
import org.jetbrains.kotlin.wasm.test.WasmCompilerInvocationTestArtifactBuilder.Companion.BIN_DIR_NAME
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import org.jetbrains.kotlin.wasm.test.tools.runCompilerViaCLI
import java.io.File

abstract class AbstractFirWasmJsKlibEvolutionTest : AbstractKlibBinaryCompatibilityTest() {
    override val extensionConfigs = EnvironmentConfigFiles.WASM_CONFIG_FILES
    override val pathToRootOutputDir = System.getProperty("kotlin.wasm.test.root.out.dir") ?: error("'kotlin.wasm.test.root.out.dir' is not set")
    override val stdlibDependency: String? = System.getProperty("kotlin.wasm.full.stdlib.path")

    private val binariesDir: File get() = File(workingDir, BIN_DIR_NAME).also { it.mkdirs() }
    private val mainModuleMjsName = "$MAIN_MODULE_NAME.mjs"
    private val mainMjs get() = File(binariesDir, mainModuleMjsName)
    private val runnerMjsPath get() = File(binariesDir, "runner.mjs")

    private fun runnerFunctionFile(): Pair<String, File> {
        val file = File(workingDir, RUNNER_FUNCTION_FILE)
        val text = runnerFileText
        file.writeText(runnerFileText)
        return text to file
    }

    override fun produceKlib(module: TestModule, version: Int) {
        // Build KLIB:
        runCompilerViaCLI(
            listOf(
                K2JSCompilerArguments::irProduceKlibFile.cliArgument,
                K2JSCompilerArguments::outputDir.cliArgument, workingDir.normalize().absolutePath,
                K2JSCompilerArguments::irModuleName.cliArgument(module.name),
                K2JSCompilerArguments::moduleName.cliArgument, module.name(version),
                K2JSCompilerArguments::wasm.cliArgument,
                K2JSCompilerArguments::libraries.cliArgument, module.dependenciesToLibrariesArg(version = version),
                *createFiles(module.versionFiles(version)).toTypedArray(),
            ),
        )
    }

    override fun produceProgram(module: TestModule) {
        assert(!module.hasVersions)

        val (text, file) = runnerFunctionFile()
        TestFile(module, file.name, text, Directives())

        produceKlib(module, version = 2)

        runCompilerViaCLI(
            listOf(
                K2JSCompilerArguments::irProduceJs.cliArgument,
                K2JSCompilerArguments::irPerModule.cliArgument,
                K2JSCompilerArguments::moduleKind.cliArgument, "plain",
                K2JSCompilerArguments::includes.cliArgument(File(workingDir, module.name(version = 2).klib).absolutePath),
                K2JSCompilerArguments::outputDir.cliArgument, binariesDir.absolutePath,
                K2JSCompilerArguments::moduleName.cliArgument, MAIN_MODULE_NAME,
                K2JSCompilerArguments::wasm.cliArgument,
                K2JSCompilerArguments::libraries.cliArgument, module.dependenciesToLibrariesArg(version = 2),
            ),
        )
        if (!mainMjs.exists()) error("Produced binary not found")

        File(runnerMjsPath.absolutePath).writeText(
            """
            const { runBoxTest } = await import('./$mainModuleMjsName');
            $RUNNER_FUNCTION();
            """,
        )
    }

    override fun runProgram(module: TestModule, expectedResult: String) {
        val result = WasmVM.V8.run(
            entryFile = runnerMjsPath.name,
            jsFiles = listOf(),
            workingDirectory = runnerMjsPath.parentFile
        )
        check("OK" == result.trim()) {
            "Running $runnerMjsPath failed: result is \"$result\""
        }
    }

    companion object {
        private val String.klib: String get() = "$this.$KLIB_FILE_EXTENSION"

        // A @JsExport wrapper for box().
        // Otherwise box() is not available in js.
        private const val RUNNER_FUNCTION = "runBoxTest"
        private const val RUNNER_FUNCTION_FILE = "boxTest.kt"
        private val runnerFileText = """
            @JsExport
            fun $RUNNER_FUNCTION() = println($TEST_FUNCTION())
        """
    }
}


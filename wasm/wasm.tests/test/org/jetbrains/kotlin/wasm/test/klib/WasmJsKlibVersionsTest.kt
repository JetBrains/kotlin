/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.js.testOld.klib.JsKlibVersionsTest
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class WasmJsKlibVersionsTest : JsKlibVersionsTest() {
    // TODO: Move to helpers in compiler/tests-common-new/tests/org/jetbrains/kotlin/test/services/KotlinStandardLibrariesPathProvider.kt
    private fun fullWasmJsStdlib(): File {
        val stdlibPath = WasmEnvironmentConfigurator.stdlibPath(WasmTarget.JS)
        return File(stdlibPath).also {
            assert(it.exists()) { "stdlib is not found at $stdlibPath" }
        }
    }

    override fun compileKlib(
        sourceFile: File,
        dependencies: Array<File>,
        outputFile: File,
        extraArgs: Array<String>,
    ): CompilationResult {
        val libraries = listOfNotNull(
            fullWasmJsStdlib(), *dependencies
        ).joinToString(File.pathSeparator) { it.absolutePath }

        val args = arrayOf(
            K2JSCompilerArguments::wasm.cliArgument,
            K2JSCompilerArguments::wasmTarget.cliArgument("wasm-js"),
            K2JSCompilerArguments::irProduceKlibDir.cliArgument,
            K2JSCompilerArguments::libraries.cliArgument, libraries,
            K2JSCompilerArguments::outputDir.cliArgument, outputFile.absolutePath,
            K2JSCompilerArguments::moduleName.cliArgument, outputFile.nameWithoutExtension,
            *extraArgs,
            sourceFile.absolutePath
        )

        val compilerXmlOutput = ByteArrayOutputStream()
        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            K2JSCompiler().execFullPathsInMessages(printStream, args)
        }

        return CompilationResult(exitCode, compilerXmlOutput.toString())
    }

    override fun compileToJs(entryModuleKlib: File, dependency: File?, outputFile: File): CompilationResult {
        val libraries = listOfNotNull(
            fullWasmJsStdlib(),
            dependency
        ).joinToString(File.pathSeparator) { it.absolutePath }

        val args = arrayOf(
            K2JSCompilerArguments::wasm.cliArgument,
            K2JSCompilerArguments::wasmTarget.cliArgument("wasm-js"),
            K2JSCompilerArguments::irProduceJs.cliArgument,
            K2JSCompilerArguments::includes.cliArgument(entryModuleKlib.absolutePath),
            K2JSCompilerArguments::libraries.cliArgument, libraries,
            K2JSCompilerArguments::outputDir.cliArgument, outputFile.absolutePath,
            K2JSCompilerArguments::moduleName.cliArgument, outputFile.nameWithoutExtension,
            K2JSCompilerArguments::target.cliArgument, "es2015",
        )

        val compilerXmlOutput = ByteArrayOutputStream()
        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            K2JSCompiler().execFullPathsInMessages(printStream, args)
        }

        return CompilationResult(exitCode, compilerXmlOutput.toString())
    }
}
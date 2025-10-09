/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.toLanguageVersionSettings
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmBackendPipelinePhase
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.loadWebKlibsInTestPipeline
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.includes
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.diagnostics.DiagnosticsCollectorStub
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.io.File


private val outputDir: File
    get() = File(System.getProperty("kotlin.wasm.test.root.out.dir") ?: error("Please set output dir path"))

const val precompiledStdlibOutputName: String = "_kotlin_"
const val precompiledKotlinTestOutputName: String = "_kotlin-test_"

val precompiledStdlibOutputDir: File
    get() = File(outputDir, "out/precompile/$precompiledStdlibOutputName")

val precompiledKotlinTestOutputDir: File
    get() = File(outputDir, "out/precompile/$precompiledKotlinTestOutputName")

val precompiledStdlibNewExceptionsOutputDir: File
    get() = File(outputDir, "out/precompile_new_exception/$precompiledStdlibOutputName")

val precompiledKotlinTestNewExceptionsOutputDir: File
    get() = File(outputDir, "out/precompile_new_exception/$precompiledKotlinTestOutputName")

fun precompileWasmModules() {
    val stdlibPath =
        File(System.getProperty("kotlin.wasm-js.stdlib.path") ?: error("Please set stdlib path")).canonicalPath
    val kotlinTestPath =
        File(System.getProperty("kotlin.wasm-js.kotlin.test.path") ?: error("Please set kotlin-test path")).canonicalPath

    val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")

    val languageSettings = K2JSCompilerArguments().toLanguageVersionSettings(
        MessageCollector.NONE,
        mapOf(allowFullyQualifiedNameInKClass to true)
    )

    val configuration = CompilerConfiguration().also {
        it.put(WasmConfigurationKeys.WASM_DEBUG, true)
        it.put(WasmConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS, true)
        it.put(WasmConfigurationKeys.WASM_TARGET, WasmTarget.JS)
        it.put(WasmConfigurationKeys.WASM_GENERATE_WAT, debugMode >= DebugMode.DEBUG)
        it.languageVersionSettings = languageSettings
    }

    val input = ConfigurationPipelineArtifact(configuration, DiagnosticsCollectorStub()) {}

    @OptIn(K1Deprecation::class)
    val environment = KotlinCoreEnvironment.createForProduction(
        input.rootDisposable,
        configuration,
        EnvironmentConfigFiles.WASM_CONFIG_FILES,
    )

    fun compileWasmModule(includes: String, libraries: List<String>, outputName: String, newExceptionProposal: Boolean, outputDir: File) {
        val klibs = loadWebKlibsInTestPipeline(
            configuration = configuration,
            includedPath = includes,
            libraryPaths = libraries,
            platformChecker = KlibPlatformChecker.Wasm(WasmTarget.JS.alias),
        )

        val module = ModulesStructure(
            project = environment.project,
            mainModule = MainModule.Klib(kotlinTestPath),
            compilerConfiguration = configuration,
            klibs = klibs,
        )

        with(configuration) {
            put<File>(JSConfigurationKeys.OUTPUT_DIR, outputDir)
            put<String>(JSConfigurationKeys.OUTPUT_NAME, outputName)
            put<Boolean>(WasmConfigurationKeys.WASM_INCLUDED_MODULE_ONLY, true)
            put<Boolean>(WasmConfigurationKeys.WASM_USE_NEW_EXCEPTION_PROPOSAL, newExceptionProposal)
            this.includes = includes
        }

        val compiledWasm = WasmBackendPipelinePhase.compileNonIncrementally(
            configuration = configuration,
            module = module,
            mainCallArguments = null
        ) ?: error("Fail to precompile $includes")

        writeCompilationResult(compiledWasm.result, compiledWasm.outputDir, outputName)

        if (debugMode >= DebugMode.DEBUG) {
            println(" ------ Wat  file://${outputDir.canonicalPath}/$outputName.wat")
            println(" ------ Wasm file://${outputDir.canonicalPath}/$outputName.wasm")
            println(" ------ JS   file://${outputDir.canonicalPath}/$outputName.uninstantiated.mjs")
        }
    }

    compileWasmModule(
        includes = stdlibPath,
        libraries = listOf(stdlibPath),
        newExceptionProposal = false,
        outputName = precompiledStdlibOutputName,
        outputDir = precompiledStdlibOutputDir,
    )
    compileWasmModule(
        includes = kotlinTestPath,
        libraries = listOf(stdlibPath, kotlinTestPath),
        newExceptionProposal = false,
        outputName = precompiledKotlinTestOutputName,
        outputDir = precompiledKotlinTestOutputDir
    )

    compileWasmModule(
        includes = stdlibPath,
        libraries = listOf(stdlibPath),
        newExceptionProposal = true,
        outputName = precompiledStdlibOutputName,
        outputDir = precompiledStdlibNewExceptionsOutputDir,
    )
    compileWasmModule(
        includes = kotlinTestPath,
        libraries = listOf(stdlibPath, kotlinTestPath),
        newExceptionProposal = true,
        outputName = precompiledKotlinTestOutputName,
        outputDir = precompiledKotlinTestNewExceptionsOutputDir,
    )
}
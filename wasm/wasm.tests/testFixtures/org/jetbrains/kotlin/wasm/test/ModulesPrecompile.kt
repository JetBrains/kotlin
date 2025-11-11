/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.K1Deprecation
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
private val stdlibPath =
    File(System.getProperty("kotlin.wasm-js.stdlib.path") ?: error("Please set stdlib path")).canonicalPath
private val kotlinTestPath =
    File(System.getProperty("kotlin.wasm-js.kotlin.test.path") ?: error("Please set kotlin-test path")).canonicalPath

const val precompiledStdlibOutputName: String = "kotlin-kotlin-stdlib"
const val precompiledKotlinTestOutputName: String = "kotlin-kotlin-test"

internal enum class PrecompileSetup(
    val debugFriendly: Boolean,
    val newExceptionProposal: Boolean,
    val stdlibOutputDir: File,
    val kotlinTestOutputDir: File,
) {
    REGULAR(
        debugFriendly = false,
        newExceptionProposal = false,
        File(outputDir, "out/precompile/$precompiledStdlibOutputName"),
        File(outputDir, "out/precompile/$precompiledKotlinTestOutputName")
    ),
    NEW_EXCEPTION_PROPOSAL(
        debugFriendly = false,
        newExceptionProposal = true,
        File(outputDir, "out/precompile_new_exception/$precompiledStdlibOutputName"),
        File(outputDir, "out/precompile_new_exception/$precompiledKotlinTestOutputName")
    ),
    DEBUG_FRIENDLY(
        debugFriendly = true,
        newExceptionProposal = false,
        stdlibOutputDir = File(outputDir, "out/precompile_debug_friendly/$precompiledStdlibOutputName"),
        kotlinTestOutputDir = File(outputDir, "out/precompile_debug_friendly/$precompiledKotlinTestOutputName")
    ),
}

internal fun precompileWasmModules(setup: PrecompileSetup) {
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
        it.put(JSConfigurationKeys.USE_DEBUGGER_CUSTOM_FORMATTERS, debugMode >= DebugMode.DEBUG)
        it.languageVersionSettings = languageSettings
    }

    val input = ConfigurationPipelineArtifact(configuration, DiagnosticsCollectorStub()) {}

    @OptIn(K1Deprecation::class)
    val environment = KotlinCoreEnvironment.createForProduction(
        input.rootDisposable,
        configuration,
        EnvironmentConfigFiles.WASM_CONFIG_FILES,
    )

    fun compileWasmModule(includes: String, libraries: List<String>, outputName: String, outputDir: File) {
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
            put<Boolean>(WasmConfigurationKeys.WASM_USE_NEW_EXCEPTION_PROPOSAL, setup.newExceptionProposal)
            put<Boolean>(WasmConfigurationKeys.WASM_FORCE_DEBUG_FRIENDLY_COMPILATION, setup.debugFriendly)
            this.includes = includes
        }

        WasmBackendPipelinePhase.compileNonIncrementally(
            configuration = configuration,
            module = module,
            mainCallArguments = null
        ) ?: error("Fail to precompile $includes")

        if (debugMode >= DebugMode.DEBUG) {
            println(" ------ Wat  file://${outputDir.canonicalPath}/$outputName.wat")
            println(" ------ Wasm file://${outputDir.canonicalPath}/$outputName.wasm")
            println(" ------ JS   file://${outputDir.canonicalPath}/$outputName.uninstantiated.mjs")
        }
    }

    compileWasmModule(
        includes = stdlibPath,
        libraries = listOf(stdlibPath),
        outputName = precompiledStdlibOutputName,
        outputDir = setup.stdlibOutputDir,
    )

    val relativeStdlibPath = setup.stdlibOutputDir.relativeTo(setup.kotlinTestOutputDir).path.replace('\\', '/')
    val rawResolutionMap = "<kotlin>:$relativeStdlibPath/$precompiledStdlibOutputName"
    with(configuration) {
        put<String>(WasmConfigurationKeys.WASM_DEPENDENCY_RESOLUTION_MAP, rawResolutionMap)
    }

    compileWasmModule(
        includes = kotlinTestPath,
        libraries = listOf(stdlibPath, kotlinTestPath),
        outputName = precompiledKotlinTestOutputName,
        outputDir = setup.kotlinTestOutputDir
    )
}
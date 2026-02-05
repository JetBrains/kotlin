/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.wasm.compileWasmIrToBinary
import org.jetbrains.kotlin.backend.wasm.linkWasmIr
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArgumentsConfigurator
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.toLanguageVersionSettings
import org.jetbrains.kotlin.cli.create
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
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.wasm.config.*
import org.jetbrains.kotlin.wasm.test.handlers.writeTo
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
        CommonCompilerArgumentsConfigurator.Reporter.DoNothing,
        mapOf(allowFullyQualifiedNameInKClass to true)
    )

    val configuration = CompilerConfiguration.create().also {
        it.wasmDebug = true
        it.wasmEnableArrayRangeChecks = true
        it.wasmCompilation = true
        it.wasmTarget = WasmTarget.JS
        it.wasmGenerateWat = debugMode >= DebugMode.DEBUG
        it.useDebuggerCustomFormatters = debugMode >= DebugMode.DEBUG
        it.languageVersionSettings = languageSettings
    }

    val input = ConfigurationPipelineArtifact(configuration) {}

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
            this.outputDir = outputDir
            this.outputName = outputName
            wasmIncludedModuleOnly = true
            wasmUseNewExceptionProposal = setup.newExceptionProposal
            wasmForceDebugFriendlyCompilation = setup.debugFriendly
            this.includes = includes
        }

        val parametersForCompile = WasmBackendPipelinePhase.compileNonIncrementally(
            configuration = configuration,
            module = module,
            mainCallArguments = null
        ).first()

        val linkedModule = linkWasmIr(parametersForCompile)
        val compileResult = compileWasmIrToBinary(parametersForCompile, linkedModule)
        compileResult.writeTo(outputDir, outputName, debugMode)
    }

    compileWasmModule(
        includes = stdlibPath,
        libraries = listOf(stdlibPath),
        outputName = precompiledStdlibOutputName,
        outputDir = setup.stdlibOutputDir,
    )

    val relativeStdlibPath = setup.stdlibOutputDir.relativeTo(setup.kotlinTestOutputDir).path.replace('\\', '/')
    val rawResolutionMap = "<kotlin>:$relativeStdlibPath/$precompiledStdlibOutputName"
    configuration.wasmDependencyResolutionMap = rawResolutionMap

    compileWasmModule(
        includes = kotlinTestPath,
        libraries = listOf(stdlibPath, kotlinTestPath),
        outputName = precompiledKotlinTestOutputName,
        outputDir = setup.kotlinTestOutputDir
    )
}

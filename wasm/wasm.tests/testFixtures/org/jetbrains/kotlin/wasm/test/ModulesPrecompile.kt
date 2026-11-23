/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArgumentsConfigurator
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.toLanguageVersionSettings
import org.jetbrains.kotlin.cli.common.testEnvironment
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.executePhaseIsolatedWithActions
import org.jetbrains.kotlin.cli.pipeline.web.wasm.*
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_WASM_JS_KOTLIN_TEST_KLIB_PATH
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_WASM_JS_STDLIB_KLIB_PATH
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.wasm.config.*
import org.jetbrains.kotlin.wasm.test.handlers.writeTo
import java.io.File
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CancellationException

private val outputDir: File
    get() = File(System.getProperty("kotlin.wasm.test.root.out.dir") ?: testInfraError("Please set output dir path"))
private val stdlibPath =
    File(System.getProperty(KOTLIN_WASM_JS_STDLIB_KLIB_PATH) ?: testInfraError("Please set stdlib path")).canonicalPath
private val kotlinTestPath =
    File(System.getProperty(KOTLIN_WASM_JS_KOTLIN_TEST_KLIB_PATH) ?: testInfraError("Please set kotlin-test path")).canonicalPath

const val precompiledStdlibOutputName: String = "kotlin-kotlin-stdlib"
const val precompiledKotlinTestOutputName: String = "kotlin-kotlin-test"

internal enum class PrecompileSetup(
    val debugFriendly: Boolean,
    val newExceptionProposal: Boolean,
    val wasmCoroutinesStackSwitching: Boolean,
    val stdlibOutputDir: File,
    val kotlinTestOutputDir: File,
) {
    REGULAR(
        debugFriendly = false,
        newExceptionProposal = false,
        wasmCoroutinesStackSwitching = false,
        File(outputDir, "out/precompile/$precompiledStdlibOutputName"),
        File(outputDir, "out/precompile/$precompiledKotlinTestOutputName")
    ),
    NEW_EXCEPTION_PROPOSAL(
        debugFriendly = false,
        newExceptionProposal = true,
        wasmCoroutinesStackSwitching = false,
        File(outputDir, "out/precompile_new_exception/$precompiledStdlibOutputName"),
        File(outputDir, "out/precompile_new_exception/$precompiledKotlinTestOutputName")
    ),
    STACK_SWITCHING_PROPOSAL(
        debugFriendly = false,
        newExceptionProposal = false,
        wasmCoroutinesStackSwitching = true,
        File(outputDir, "out/precompile_stack_switching/$precompiledStdlibOutputName"),
        File(outputDir, "out/precompile_stack_switching/$precompiledKotlinTestOutputName")
    ),
    DEBUG_FRIENDLY(
        debugFriendly = true,
        newExceptionProposal = false,
        wasmCoroutinesStackSwitching = false,
        stdlibOutputDir = File(outputDir, "out/precompile_debug_friendly/$precompiledStdlibOutputName"),
        kotlinTestOutputDir = File(outputDir, "out/precompile_debug_friendly/$precompiledKotlinTestOutputName")
    ),
    ;

    private var precompiled = false
    private var failure: Throwable? = null

    @Synchronized
    internal fun precompileOnce() {
        if (precompiled) return
        // A fresh exception per call, since the cached one is shared by all the tests which run into it.
        failure?.let { throw IllegalStateException("The precompilation of the $name setup has failed", it) }
        try {
            precompileWasmModulesNow(this)
            precompiled = true
        } catch (e: Throwable) {
            // An interruption or a cancellation of the calling test, or a shortage of the JVM like an `OutOfMemoryError`
            // while other batches compile, says nothing of the precompilation itself, so it is retried, also when the
            // compiler wraps it into another exception. A `StackOverflowError` is not: it recurs on every attempt.
            val isTransient = e.hasTransientCause() || Thread.currentThread().isInterrupted
            if (!isTransient) failure = e
            throw e
        }
    }
}

/** Whether this or one of its causes is an interruption, a cancellation or an [OutOfMemoryError]. */
private fun Throwable.hasTransientCause(): Boolean {
    // The identity set stops the walk at a cycle of causes.
    val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    return generateSequence(this) { it.cause }.takeWhile(seen::add).any {
        it is InterruptedException || it is CancellationException || it is OutOfMemoryError
    }
}

/**
 * Precompiles the stdlib and kotlin-test of [setup] once per JVM. A failure is rethrown on every later call instead of
 * being retried, so that a broken precompilation fails each test fast rather than repeating the whole work per module;
 * only a failure caused by an interruption, a cancellation or an [OutOfMemoryError] is retried.
 */
internal fun precompileWasmModules(setup: PrecompileSetup): Unit = setup.precompileOnce()

private fun precompileWasmModulesNow(setup: PrecompileSetup) {
    val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")

    val languageSettings = KotlinWasmCompilerArguments().toLanguageVersionSettings(
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
        it.testEnvironment = true
    }

    val input = ConfigurationPipelineArtifact(configuration) {}

    fun compileWasmModule(includes: String, libraries: List<String>, outputName: String, outputDir: File) {
        with(configuration) {
            this.outputDir = outputDir
            this.outputName = outputName
            wasmIncludedModuleOnly = true
            wasmUseNewExceptionProposal = setup.newExceptionProposal
            wasmForceDebugFriendlyCompilation = setup.debugFriendly
            wasmUseStackSwitchingProposal = setup.wasmCoroutinesStackSwitching
            this.libraries = libraries
            this.includes = includes
        }

        val compileResult = (WasmIrLoadingPipelinePhase then
                WasmIrLinkingPipelinePhase then
                WasmIrLoweringPipelinePhase then
                WasmSingleModuleBackendIrGenerationPipelinePhase then
                WasmOutputGenerationPipelinePhase)
            .executePhaseIsolatedWithActions(input)!!
            .result
            .single()
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

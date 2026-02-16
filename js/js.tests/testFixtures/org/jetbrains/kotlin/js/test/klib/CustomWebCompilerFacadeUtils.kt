/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.frontend.fir.getTransitivesAndFriends
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.targetPlatformProvider
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.jetbrains.kotlin.wasm.config.wasmTarget
import java.io.File

/**
 * Note: To be used only internally in [CustomWebCompilerFirstStageFacade].
 */
internal fun TestModule.isWasmModule(testServices: TestServices): Boolean =
    testServices.targetPlatformProvider.getTargetPlatform(module = this).isWasm()

/**
 * Returns null only if this is a JS target.
 *
 * Note: To be used only internally in [CustomWebCompilerFirstStageFacade].
 */
internal fun TestModule.wasmTargetOrNull(testServices: TestServices, compilationStage: CompilationStage): WasmTarget? =
    runIf(isWasmModule(testServices)) {
        testServices.compilerConfigurationProvider.getCompilerConfiguration(module = this, compilationStage).wasmTarget
    }

/**
 * Note: To be used only internally in [CustomWebCompilerFirstStageFacade] and [CustomWebCompilerSecondStageEnvironmentConfigurator].
 */
internal fun TestModule.customWebCompilerSettings(testServices: TestServices): CustomWebCompilerSettings =
    if (isWasmModule(testServices)) customWasmJsCompilerSettings else customJsCompilerSettings

/**
 * Note: To be used only internally in [CustomWebCompilerFirstStageFacade] and [CustomJsCompilerSecondStageFacade].
 */
fun TestModule.collectDependencies(
    testServices: TestServices,
    compilationStage: CompilationStage,
): Pair<Set<String>, Set<String>> {
    val runtimeLibraries: List<File> = when (wasmTargetOrNull(testServices, compilationStage)) {
        null -> { // JS
            // For proper compilation by the old compiler version, `kotlin-stdlib-js-ir-minimal-for-test` cannot be used from the current Kotlin repo.
            // Instead, stdlib and `kotlin.test` from old dist must be used for all tests (`kotlin.test` is not part of `kotlin-stdlib-js-*.klib`)
            listOf(
                customJsCompilerSettings.stdlib,
                customJsCompilerSettings.kotlinTest,
            )
        }
        WasmTarget.JS -> {
            listOf(
                customWasmJsCompilerSettings.stdlib,
                customWasmJsCompilerSettings.kotlinTest,
            )
        }
        WasmTarget.WASI -> error("WASI target is not yet supported in the first phase of ${CustomWebCompilerFirstStageFacade::class.simpleName}")
    }

    val (transitiveLibraries: List<File>, friendLibraries: List<File>) = getTransitivesAndFriends(module = this, testServices)

    val regularDependencies: Set<String> = buildSet {
        runtimeLibraries.mapTo(this) { it.absolutePath }
        transitiveLibraries.mapTo(this) { it.absolutePath }
    }

    val friendDependencies: Set<String> = friendLibraries.mapToSetOrEmpty { it.absolutePath }

    return regularDependencies to friendDependencies
}

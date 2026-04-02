/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.compiler.plugin.registerExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.incremental.utils.findAnnotationsRuntimeWasmKlib
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.plugin.sandbox.fir.FirPluginPrototypeExtensionRegistrar
import org.jetbrains.kotlin.plugin.sandbox.ir.GeneratedDeclarationsIrBodyFiller
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.wasm.config.wasmGenerateClosedWorldMultimodule
import org.jetbrains.kotlin.wasm.config.wasmIncludedModuleOnly

abstract class AbstractIncrementalWasmWithPluginCompilerRunnerTest(
    workingDirPath: String,
) : WasmAbstractInvalidationTest(TargetBackend.WASM, workingDirPath) {
    private val annotationsKlib = findAnnotationsRuntimeWasmKlib()

    override val librariesToExcludeFromStats
        get() = super.librariesToExcludeFromStats + annotationsKlib

    @OptIn(ExperimentalCompilerApi::class)
    override fun createConfiguration(
        moduleName: String,
        moduleKind: ModuleKind,
        languageFeatures: List<String>,
        allLibraries: List<String>,
        friendLibraries: List<String>,
        includedLibrary: String?,
    ): CompilerConfiguration {
        val copy = super.createConfiguration(
            moduleName,
            moduleKind,
            languageFeatures,
            allLibraries + annotationsKlib,
            friendLibraries,
            includedLibrary
        )
        with(copy.extensionsStorage!!) {
            // Since the IC infrastructure is weird and duplicate emitting of extensions
            if (registeredExtensions.isEmpty()) {
                FirExtensionRegistrar.registerExtension(FirPluginPrototypeExtensionRegistrar())
                IrGenerationExtension.registerExtension(GeneratedDeclarationsIrBodyFiller())
            }
        }
        return copy
    }
}

abstract class AbstractIncrementalWasmWithPluginSandboxTest :
    AbstractIncrementalWasmWithPluginCompilerRunnerTest("plugin-sandbox/incremental/wasm")

abstract class AbstractIncrementalWasmMultiModuleWithPluginSandboxTest :
    AbstractIncrementalWasmWithPluginCompilerRunnerTest("plugin-sandbox/incremental/wasmMultimodule") {
    override fun modifyConfig(configuration: CompilerConfiguration) {
        configuration.wasmGenerateClosedWorldMultimodule = true
    }
}

abstract class AbstractIncrementalWasmSingleModuleWithPluginSandboxTest :
    AbstractIncrementalWasmWithPluginCompilerRunnerTest("plugin-sandbox/incremental/wasmSinglemodule") {
    override fun modifyConfig(configuration: CompilerConfiguration) {
        configuration.wasmIncludedModuleOnly = true
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime.pluginSandboxAnnotationsJsForTests
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.compiler.plugin.registerExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.plugin.sandbox.fir.FirPluginPrototypeExtensionRegistrar
import org.jetbrains.kotlin.plugin.sandbox.ir.GeneratedDeclarationsIrBodyFiller
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File

abstract class AbstractIncrementalCodegenJsWithPluginCompilerRunnerTest(
    targetBackend: TargetBackend,
    granularity: JsGenerationGranularity,
    workingDirPath: String
) : JsAbstractInvalidationTest(targetBackend, granularity, workingDirPath) {
    private val annotationsKlib = pluginSandboxAnnotationsJsForTests().path

    override val librariesToExcludeFromStats
        get() = super.librariesToExcludeFromStats + annotationsKlib

    override val libraryNamesToExcludeFromStats: Set<String>
        get() = super.libraryNamesToExcludeFromStats + "kotlin_org_jetbrains_kotlin_plugin_annotations"

    @OptIn(ExperimentalCompilerApi::class)
    override fun createConfiguration(
        moduleName: String,
        moduleKind: ModuleKind,
        languageFeatures: List<String>,
        allLibraries: List<String>,
        friendLibraries: List<String>,
        includedLibrary: String?,
        outputDir: File
    ): CompilerConfiguration {
        val copy = super.createConfiguration(
            moduleName,
            moduleKind,
            languageFeatures,
            allLibraries + annotationsKlib,
            friendLibraries,
            includedLibrary,
            outputDir
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

abstract class AbstractIncrementalCodegenJsWithPluginSandboxPerModuleTest :
    AbstractIncrementalCodegenJsWithPluginCompilerRunnerTest(
        TargetBackend.JS_IR,
        JsGenerationGranularity.PER_MODULE,
        "plugin-sandbox/incremental/perModule"
    )

abstract class AbstractIncrementalCodegenJsEs6WithPluginSandboxPerModuleTest :
    AbstractIncrementalCodegenJsWithPluginCompilerRunnerTest(
        TargetBackend.JS_IR_ES6,
        JsGenerationGranularity.PER_MODULE,
        "plugin-sandbox/incremental/perModuleEs6"
    )

abstract class AbstractIncrementalCodegenJsWithPluginSandboxPerFileTest :
    AbstractIncrementalCodegenJsWithPluginCompilerRunnerTest(
        TargetBackend.JS_IR,
        JsGenerationGranularity.PER_FILE,
        "plugin-sandbox/incremental/perFile"
    )

abstract class AbstractIncrementalCodegenJsEs6WithPluginSandboxPerFileTest :
    AbstractIncrementalCodegenJsWithPluginCompilerRunnerTest(
        TargetBackend.JS_IR_ES6,
        JsGenerationGranularity.PER_FILE,
        "plugin-sandbox/incremental/perFileEs6"
    )

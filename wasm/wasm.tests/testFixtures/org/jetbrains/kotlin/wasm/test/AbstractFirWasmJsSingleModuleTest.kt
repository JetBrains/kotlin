/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.WASM_FAILS_IN_SINGLE_MODULE_MODE
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.AnalysisHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.wasm.test.converters.WasmBackendSingleModuleFacade
import org.jetbrains.kotlin.wasm.test.handlers.WasmBoxRunnerWithPrecompiled
import org.jetbrains.kotlin.wasm.test.handlers.WasmDebugRunner
import org.jetbrains.kotlin.wasm.test.handlers.WasmDebugRunnerBase
import org.jetbrains.kotlin.wasm.test.handlers.getWasmTestOutputDirectory
import org.jetbrains.kotlin.wasm.test.providers.WasmJsSteppingTestAdditionalSourceProvider
import org.junit.jupiter.api.BeforeAll
import java.io.File


abstract class AbstractWasmJsCodegenSingleModuleTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) : AbstractFirWasmTest(
    targetBackend = TargetBackend.WASM_JS,
    targetPlatform = WasmPlatforms.wasmJs,
    pathToTestDir = pathToTestDir,
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    companion object {
        @JvmStatic
        private var precompileIsDone = false

        @BeforeAll
        @JvmStatic
        @Synchronized
        fun precompileTestDependencies() {
            if (!precompileIsDone) {
                precompileWasmModules()
                precompileIsDone = true
            }
        }
    }

    override val wasmBoxTestRunner: Constructor<AnalysisHandler<BinaryArtifacts.Wasm>>
        get() = ::WasmBoxRunnerWithPrecompiled

    override val afterBackendFacade: Constructor<AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Wasm>>
        get() = ::WasmBackendSingleModuleFacade

    override val wasmTarget: WasmTarget
        get() = WasmTarget.JS

    private class IgnoredTestSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
        override val directiveContainers: List<DirectivesContainer>
            get() = listOf(WasmEnvironmentConfigurationDirectives)

        override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
            val suppressed = testServices.moduleStructure.modules.any { WASM_FAILS_IN_SINGLE_MODULE_MODE in it.directives }
            if (!suppressed) return failedAssertions
            if (failedAssertions.isNotEmpty()) return emptyList()

            return listOf(AssertionError("Looks like this test can be unmuted. Remove WASM_FAILS_IN_SINGLE_MODULE_MODE directive").wrap())
        }
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureCodegenFirHandlerSteps()
        builder.useAfterAnalysisCheckers(
            ::IgnoredTestSuppressor,
        )
    }
}

open class AbstractFirWasmJsCodegenSingleModuleBoxTest(
    testGroupOutputDirPrefix: String = "codegen/singleModuleBox/"
) : AbstractWasmJsCodegenSingleModuleTest(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
)

open class AbstractFirWasmJsCodegenSingleModuleInteropTest : AbstractWasmJsCodegenSingleModuleTest(
    pathToTestDir = "compiler/testData/codegen/boxWasmJsInterop",
    testGroupOutputDirPrefix = "codegen/wasmJsSingleModuleInterop"
)

open class AbstractFirWasmTypeScriptExportSingleModuleTest : AbstractWasmJsCodegenSingleModuleTest(
    "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/typescript-export/wasm/",
    "typescript-export-single-module/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            +WasmEnvironmentConfigurationDirectives.CHECK_TYPESCRIPT_DECLARATIONS
        }
    }
}

class WasmDebugSingleModuleRunner(testServices: TestServices) : WasmDebugRunnerBase(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            val artifacts = modulesToArtifact.values.single()
            val outputDirBase = testServices.getWasmTestOutputDirectory()
            writeToFilesAndRunTest(
                outputDir = outputDirBase,
                res = artifacts.compilerResult
            )
        }
    }
}

open class AbstractFirWasmJsSteppingSingleFileTest(
    testGroupOutputDirPrefix: String = "debug/stepping/firBoxSingleModule"
) : AbstractWasmJsCodegenSingleModuleTest(
    "compiler/testData/debug/stepping/",
    testGroupOutputDirPrefix
) {
    override val wasmBoxTestRunner: Constructor<AnalysisHandler<BinaryArtifacts.Wasm>>
        get() = ::WasmDebugSingleModuleRunner

//    override fun configure(builder: TestConfigurationBuilder) {
//        super.configure(builder)
//        with(builder) {
//            useAdditionalSourceProviders(::WasmJsSteppingTestAdditionalSourceProvider)
//            defaultDirectives {
//                +WasmEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP
//                +WasmEnvironmentConfigurationDirectives.FORCE_DEBUG_FRIENDLY_COMPILATION
//                +WasmEnvironmentConfigurationDirectives.SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES
//            }
//        }
//    }
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.WASM_FAILS_IN_SINGLE_MODULE_MODE
import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.AnalysisHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.enableByConfigurationKey
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys.WASM_INCLUDED_MODULE_ONLY
import org.jetbrains.kotlin.wasm.test.converters.WasmBackendSingleModuleFacade
import org.jetbrains.kotlin.wasm.test.handlers.WasmBoxRunnerWithPrecompiled
import org.jetbrains.kotlin.wasm.test.handlers.WasmDebugRunnerWithPrecompiled
import org.jetbrains.kotlin.wasm.test.providers.WasmJsSteppingTestAdditionalSourceProvider
import org.jetbrains.kotlin.wasm.test.utils.configureIgnoredTestSuppressor
import org.junit.jupiter.api.BeforeAll

abstract class AbstractWasmJsCodegenSingleModuleRegularStdTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) : AbstractWasmJsCodegenSingleModuleTestBase(pathToTestDir, testGroupOutputDirPrefix) {
    companion object {
        @JvmStatic
        private var precompileIsDone = false

        @BeforeAll
        @JvmStatic
        @Synchronized
        fun precompileTestDependencies() {
            if (!precompileIsDone) {
                precompileWasmModules(PrecompileSetup.REGULAR)
                precompileWasmModules(PrecompileSetup.NEW_EXCEPTION_PROPOSAL)
                precompileIsDone = true
            }
        }
    }
}

abstract class AbstractWasmJsCodegenSingleModuleTestBase(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) : AbstractFirWasmTest(
    targetBackend = TargetBackend.WASM_JS,
    targetPlatform = WasmPlatforms.wasmJs,
    pathToTestDir = pathToTestDir,
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    override val wasmBoxTestRunner: Constructor<AnalysisHandler<BinaryArtifacts.Wasm>>
        get() = ::WasmBoxRunnerWithPrecompiled

    override val afterBackendFacade: Constructor<AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Wasm>>
        get() = ::WasmBackendSingleModuleFacade

    override val wasmTarget: WasmTarget
        get() = WasmTarget.JS

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureCodegenFirHandlerSteps()
        builder.configureIgnoredTestSuppressor(WASM_FAILS_IN_SINGLE_MODULE_MODE)
        builder.enableByConfigurationKey(WASM_INCLUDED_MODULE_ONLY)
    }
}

open class AbstractFirWasmJsCodegenSingleModuleBoxTest(
    testGroupOutputDirPrefix: String = "codegen/singleModuleBox/",
) : AbstractWasmJsCodegenSingleModuleRegularStdTest(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
)

open class AbstractFirWasmJsCodegenSingleModuleInteropTest : AbstractWasmJsCodegenSingleModuleRegularStdTest(
    pathToTestDir = "compiler/testData/codegen/boxWasmJsInterop",
    testGroupOutputDirPrefix = "codegen/wasmJsSingleModuleInterop"
)

open class AbstractFirWasmTypeScriptExportSingleModuleTest : AbstractWasmJsCodegenSingleModuleRegularStdTest(
    "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/typescript-export/wasm/",
    "typescript-export-single-module/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            +WasmEnvironmentConfigurationDirectives.CHECK_TYPESCRIPT_DECLARATIONS
            JsEnvironmentConfigurationDirectives.TSC_TARGET with "es2020"
            JsEnvironmentConfigurationDirectives.TSC_MODULE with "es2020"
        }
    }
}

open class AbstractFirWasmJsSteppingSingleModuleTest(
    testGroupOutputDirPrefix: String = "debug/stepping/firBoxSingleModule",
) : AbstractWasmJsCodegenSingleModuleTestBase(
    "compiler/testData/debug/stepping/",
    testGroupOutputDirPrefix
) {
    override val wasmBoxTestRunner: Constructor<AnalysisHandler<BinaryArtifacts.Wasm>>
        get() = ::WasmDebugRunnerWithPrecompiled

    companion object {
        @JvmStatic
        private var precompileIsDone = false

        @BeforeAll
        @JvmStatic
        @Synchronized
        fun precompileTestDependencies() {
            if (!precompileIsDone) {
                precompileWasmModules(PrecompileSetup.DEBUG_FRIENDLY)
                precompileIsDone = true
            }
        }
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useAdditionalSourceProviders(::WasmJsSteppingTestAdditionalSourceProvider)
            defaultDirectives {
                +WasmEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP
                +WasmEnvironmentConfigurationDirectives.FORCE_DEBUG_FRIENDLY_COMPILATION
                +WasmEnvironmentConfigurationDirectives.SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES
            }
        }
    }
}
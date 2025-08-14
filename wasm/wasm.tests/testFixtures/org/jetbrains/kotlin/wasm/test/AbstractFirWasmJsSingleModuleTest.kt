/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.WASM_FAILS_IN_SINGLE_MODULE_MODE
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.AnalysisHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfiguratorJs
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.wasm.test.converters.SingleModuleType
import org.jetbrains.kotlin.wasm.test.converters.WasmBackendSingleModuleFacade
import org.jetbrains.kotlin.wasm.test.handlers.PrecompiledWasmSaver
import org.jetbrains.kotlin.wasm.test.handlers.WasmBoxRunnerWithPrecompiled

abstract class FirWasmJsSingleModuleBase(
    private val singleModuleType: SingleModuleType,
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) : AbstractFirWasmTest(
    WasmPlatforms.wasmJs,
    pathToTestDir,
    testGroupOutputDirPrefix,
) {
    override val wasmBoxTestRunner: Constructor<AnalysisHandler<BinaryArtifacts.Wasm>>
        get() = if (singleModuleType != SingleModuleType.TEST_MODULE) ::PrecompiledWasmSaver else ::WasmBoxRunnerWithPrecompiled

    override val wasmEnvironmentConfigurator: Constructor<EnvironmentConfigurator>
        get() = ::WasmEnvironmentConfiguratorJs

    override val afterBackendFacade: Constructor<AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Wasm>>
        get() = { services -> WasmBackendSingleModuleFacade(services, singleModuleType) }
}

open class AbstractFirWasmJsPrecompiledStdlib : FirWasmJsSingleModuleBase(
    singleModuleType = SingleModuleType.STDLIB,
    pathToTestDir = "wasm/wasm.tests/precompile",
    testGroupOutputDirPrefix = "precompile"
)

open class AbstractFirWasmJsPrecompiledKotlinTest : FirWasmJsSingleModuleBase(
    singleModuleType = SingleModuleType.KOTLIN_TEST,
    pathToTestDir = "wasm/wasm.tests/precompile",
    testGroupOutputDirPrefix = "precompile"
)

open class AbstractFirWasmJsCodegenSingleModuleBoxTest(
    testGroupOutputDirPrefix: String = "codegen/firBox/"
) : FirWasmJsSingleModuleBase(
    SingleModuleType.TEST_MODULE,
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    private class IgnoredTestSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
        override val directiveContainers: List<DirectivesContainer>
            get() = listOf(WasmEnvironmentConfigurationDirectives)

        override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> =
            failedAssertions
                .takeIf { testServices.moduleStructure.modules.none { WASM_FAILS_IN_SINGLE_MODULE_MODE in it.directives } }
                ?: emptyList()
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }

        builder.useAfterAnalysisCheckers(
            ::IgnoredTestSuppressor, ::FirMetaInfoDiffSuppressor,
        )
    }
}

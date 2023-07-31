/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfiguratorWasi
import org.jetbrains.kotlin.wasm.test.converters.FirWasmKlibBackendFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmBackendFacade
import org.jetbrains.kotlin.wasm.test.handlers.WasiBoxRunner

abstract class AbstractK1WasmWasiTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) : AbstractWasmBlackBoxCodegenTestBase<ClassicFrontendOutputArtifact, IrBackendInput, BinaryArtifacts.KLib>(
    FrontendKinds.ClassicFrontend, TargetBackend.WASM, pathToTestDir, testGroupOutputDirPrefix
) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::FirWasmKlibBackendFacade

    override val afterBackendFacade: Constructor<AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Wasm>>
        get() = ::WasmBackendFacade

    override val wasmBoxTestRunner: Constructor<AnalysisHandler<BinaryArtifacts.Wasm>>
        get() = ::WasiBoxRunner

    override val wasmEnvironmentConfigurator: Constructor<EnvironmentConfigurator>
        get() = ::WasmEnvironmentConfiguratorWasi

    override val additionalSourceProvider: Constructor<AdditionalSourceProvider>?
        get() = ::WasmWasiBoxTestHelperSourceProvider
}

open class AbstractK1WasmWasiCodegenBoxTest : AbstractK1WasmWasiTest(
    "compiler/testData/codegen/boxWasmWasi/",
    "codegen/k1WasmWasiBox"
)

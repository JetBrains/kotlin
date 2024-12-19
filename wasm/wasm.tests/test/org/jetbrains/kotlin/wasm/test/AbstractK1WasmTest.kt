/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfiguratorJs
import org.jetbrains.kotlin.wasm.test.converters.FirWasmKlibSerializerFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmBackendFacade
import org.jetbrains.kotlin.wasm.test.handlers.WasmBoxRunner
import org.jetbrains.kotlin.wasm.test.handlers.WasmDebugRunner
import org.jetbrains.kotlin.wasm.test.providers.WasmJsSteppingTestAdditionalSourceProvider

abstract class AbstractK1WasmTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) : AbstractWasmBlackBoxCodegenTestBase<ClassicFrontendOutputArtifact, IrBackendInput, BinaryArtifacts.KLib>(
    FrontendKinds.ClassicFrontend, TargetBackend.WASM, WasmPlatforms.wasmJs, pathToTestDir, testGroupOutputDirPrefix
) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::FirWasmKlibSerializerFacade

    override val afterBackendFacade: Constructor<AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Wasm>>
        get() = ::WasmBackendFacade

    override val wasmBoxTestRunner: Constructor<AnalysisHandler<BinaryArtifacts.Wasm>>
        get() = ::WasmBoxRunner

    override val wasmEnvironmentConfigurator: Constructor<EnvironmentConfigurator>
        get() = ::WasmEnvironmentConfiguratorJs
}

open class AbstractK1WasmCodegenBoxTest : AbstractK1WasmTest(
    "compiler/testData/codegen/box/",
    "codegen/k1Box/"
)

open class AbstractK1WasmCodegenBoxInlineTest : AbstractK1WasmTest(
    "compiler/testData/codegen/boxInline/",
    "codegen/k1BoxInline/"
)

open class AbstractK1WasmCodegenWasmJsInteropTest : AbstractK1WasmTest(
    "compiler/testData/codegen/wasmJsInterop",
    "codegen/k1WasmJsInteropBox"
)

open class AbstractK1WasmJsTranslatorTest : AbstractK1WasmTest(
    "js/js.translator/testData/box/",
    "js.translator/k1Box"
)

open class AbstractK1WasmSteppingTest : AbstractK1WasmTest(
    "compiler/testData/debug/stepping/",
    "debug/stepping/k1Box"
) {

    override val wasmBoxTestRunner: Constructor<AnalysisHandler<BinaryArtifacts.Wasm>>
        get() = ::WasmDebugRunner

    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForWasmBlackBoxCodegenTest()
        useAdditionalSourceProviders(::WasmJsSteppingTestAdditionalSourceProvider)
        defaultDirectives {
            +WasmEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP
            +WasmEnvironmentConfigurationDirectives.SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES
        }
    }
}

open class AbstractK1WasmTypeScriptExportTest : AbstractK1WasmTest(
    "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/typescript-export/wasm/",
    "typescript-export/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            +WasmEnvironmentConfigurationDirectives.CHECK_TYPESCRIPT_DECLARATIONS
        }
    }
}

open class AbstractFirWasmTypeScriptExportTest : AbstractK1WasmTest(
    "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/typescript-export/wasm/",
    "typescript-export/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            +WasmEnvironmentConfigurationDirectives.CHECK_TYPESCRIPT_DECLARATIONS
        }
    }
}

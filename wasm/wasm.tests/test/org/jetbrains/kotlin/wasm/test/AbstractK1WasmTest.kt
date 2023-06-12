/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.wasm.test.converters.FirWasmKlibBackendFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmBackendFacade

abstract class AbstractK1WasmTest(
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

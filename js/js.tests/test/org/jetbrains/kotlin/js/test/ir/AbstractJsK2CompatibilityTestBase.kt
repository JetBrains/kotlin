/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import org.jetbrains.kotlin.js.test.converters.JsKlibBackendFacade
import org.jetbrains.kotlin.js.test.converters.JsLazyIrFromKlibLoader
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrJsResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.BackendFacade
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.runners.codegen.FirPsiCodegenTest
import org.jetbrains.kotlin.test.runners.ir.AbstractClassicToK2CompatibilityTest
import org.jetbrains.kotlin.test.runners.ir.AbstractFirToK2CompatibilityTest

open class AbstractClassicJsToK2CompatibilityTest :
    AbstractClassicToK2CompatibilityTest<BinaryArtifacts.KLib>(JsPlatforms.defaultJsPlatform, TargetBackend.JS_IR) {

    override val frontendToBackend: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::JsKlibBackendFacade

    override val deserializedLazyIrFacade: Constructor<AbstractTestFacade<BinaryArtifacts.KLib, IrBackendInput>>
        get() = ::JsLazyIrFromKlibLoader
}

open class AbstractFirLightTreeJsToK2CompatibilityTest :
    AbstractFirToK2CompatibilityTest<BinaryArtifacts.KLib>(JsPlatforms.defaultJsPlatform, TargetBackend.JS_IR, FirParser.LightTree) {

    override val frontendToBackend: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrJsResultsConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::JsKlibBackendFacade

    override val deserializedLazyIrFacade: Constructor<AbstractTestFacade<BinaryArtifacts.KLib, IrBackendInput>>
        get() = ::JsLazyIrFromKlibLoader
}

@FirPsiCodegenTest
open class AbstractFirPsiJsToK2CompatibilityTest :
    AbstractFirToK2CompatibilityTest<BinaryArtifacts.KLib>(JsPlatforms.defaultJsPlatform, TargetBackend.JS_IR, FirParser.Psi) {

    override val frontendToBackend: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrJsResultsConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::JsKlibBackendFacade

    override val deserializedLazyIrFacade: Constructor<AbstractTestFacade<BinaryArtifacts.KLib, IrBackendInput>>
        get() = ::JsLazyIrFromKlibLoader
}

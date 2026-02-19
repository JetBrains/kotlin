/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.commonConfigurationForDumpSyntheticAccessorsTest
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.BackendFacade
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.IrPreSerializationLoweringFacade
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.WasmFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlin.wasm.test.converters.FirWasmKlibSerializerFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmDeserializerFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmPreSerializationLoweringFacade

open class AbstractWasmJsKlibSyntheticAccessorTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.WASM) {
    val targetFrontend = FrontendKinds.FIR
    val parser = FirParser.LightTree
    val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade // TODO Change for ::FirCliWebFacade in scope of KT-74671
    val frontendToIrConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrResultsConverter // TODO Change for ::Fir2IrCliWebFacade in scope of KT-74671
    val irInliningFacade: Constructor<IrPreSerializationLoweringFacade<IrBackendInput>>
        get() = ::WasmPreSerializationLoweringFacade
    val serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::FirWasmKlibSerializerFacade // TODO Change for ::FirKlibSerializerCliWebFacade in scope of KT-74671
    val deserializerFacade: Constructor<org.jetbrains.kotlin.test.model.DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>>
        get() = ::WasmDeserializerFacade

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForDumpSyntheticAccessorsTest(
            targetFrontend,
            frontendFacade,
            frontendToIrConverter,
            irInliningFacade,
            serializerFacade,
            deserializerFacade,
            KlibBasedCompilerTestDirectives.IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS,
        )
        globalDefaults {
            targetPlatform = WasmPlatforms.wasmJs
        }
        useConfigurators(
            ::WasmFirstStageEnvironmentConfigurator.bind(WasmTarget.JS),
        )
        configureFirParser(parser)
    }
}


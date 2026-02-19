/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.AbstractKlibAbiDumpBeforeInliningSavingHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.KlibFacades
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.additionalK2ConfigurationForIrTextTest
import org.jetbrains.kotlin.test.directives.KlibAbiConsistencyDirectives.CHECK_SAME_ABI_AFTER_INLINING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.IrPreSerializationLoweringFacade
import org.jetbrains.kotlin.test.runners.ir.AbstractNonJvmIrTextTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlin.wasm.test.converters.FirWasmKlibSerializerFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmDeserializerFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmPreSerializationLoweringFacade
import org.jetbrains.kotlin.wasm.test.handlers.FirWasmJsKlibAbiDumpBeforeInliningSavingHandler

abstract class AbstractWasmJsIrTextTest :
    AbstractNonJvmIrTextTest<FirOutputArtifact>(WasmPlatforms.wasmJs, TargetBackend.WASM_JS) {
    override val frontend: FrontendKind<*>
        get() = FrontendKinds.FIR

    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade // TODO Change for ::FirCliWebFacade in scope of KT-74671

    override val converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrResultsConverter // TODO Change for ::Fir2IrCliWebFacade in scope of KT-74671

    override val preSerializerFacade: Constructor<IrPreSerializationLoweringFacade<IrBackendInput>>
        get() = ::WasmPreSerializationLoweringFacade

    override val klibAbiDumpBeforeInliningSavingHandler: Constructor<AbstractKlibAbiDumpBeforeInliningSavingHandler>?
        get() = ::FirWasmJsKlibAbiDumpBeforeInliningSavingHandler

    override val klibFacades: KlibFacades
        get() = KlibFacades(
            serializerFacade = ::FirWasmKlibSerializerFacade, // TODO Change for ::FirKlibSerializerCliWebFacade in scope of KT-74671
            deserializerFacade = ::WasmDeserializerFacade,
        )

    final override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::WasmFirstStageEnvironmentConfigurator.bind(WasmTarget.JS),
        )

        useAdditionalService(::LibraryProvider)
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.additionalK2ConfigurationForIrTextTest(FirParser.LightTree)
        with(builder) {
            defaultDirectives {
                +CHECK_SAME_ABI_AFTER_INLINING
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
            }
        }
    }
}

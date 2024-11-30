/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.serialization

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.FirNativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformer
import org.jetbrains.kotlin.konan.test.converters.NativeDeserializerFacade
import org.jetbrains.kotlin.konan.test.converters.NativeInliningFacade
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.KlibFacades
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgConsistencyHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolvedTypesVerifier
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.codegen.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.utils.bind

// Base class for IR serialization/deserialization test, configured with FIR frontend, in Native-specific way.
open class AbstractFirNativeSerializationTest :
    AbstractNativeSerializationTestBase<FirOutputArtifact, IrBackendInput, BinaryArtifacts.KLib>(
        FrontendKinds.FIR,
        TargetBackend.NATIVE
    ) {
    val parser = FirParser.LightTree
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade
    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrNativeResultsConverter
    override val irInliningFacade: Constructor<IrInliningFacade<IrBackendInput>>
        // KT-73624: TODO In a new sub-class AbstractFirNativeSerializationWithInlinedFunInKlibTest, bind NativePreSerializationLoweringPhasesProvider instead
        get() = ::NativeInliningFacade.bind(null)
    override val klibFacades: KlibFacades?
        get() = KlibFacades(
            serializerFacade = ::FirNativeKlibSerializerFacade,
            deserializerFacade = ::NativeDeserializerFacade,
        )

    /**
     * Called directly from test class constructor.
     */
    fun register(@TestDataFile testDataFilePath: String, sourceTransformer: ExternalSourceTransformer) {}

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +ConfigurationDirectives.WITH_STDLIB
            }
            firHandlersStep {
                configureFirParser(parser)
                commonFirHandlersForCodegenTest()
                useHandlers(
                    ::FirDumpHandler,
                    ::FirCfgDumpHandler,
                    ::FirCfgConsistencyHandler,
                    ::FirResolvedTypesVerifier,
                )
            }
            useAfterAnalysisCheckers(
                ::FirMetaInfoDiffSuppressor
            )
        }
    }
}


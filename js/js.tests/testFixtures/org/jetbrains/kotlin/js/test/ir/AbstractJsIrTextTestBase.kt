/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import org.jetbrains.kotlin.js.test.converters.JsIrDeserializerFacade
import org.jetbrains.kotlin.js.test.converters.JsIrPreSerializationLoweringFacade
import org.jetbrains.kotlin.js.test.converters.ClassicJsKlibSerializerFacade
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.KlibFacades
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.ir.AbstractNonJvmIrTextTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsSecondStageEnvironmentConfigurator

abstract class AbstractJsIrTextTestBase<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>> :
    AbstractNonJvmIrTextTest<FrontendOutput>(JsPlatforms.defaultJsPlatform, TargetBackend.JS_IR) {
    override val preSerializerFacade: Constructor<IrPreSerializationLoweringFacade<IrBackendInput>>
        get() = ::JsIrPreSerializationLoweringFacade

    final override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsFirstStageEnvironmentConfigurator,
            ::JsSecondStageEnvironmentConfigurator,
        )

        useAdditionalService(::LibraryProvider)
    }
}

open class AbstractClassicJsIrTextTest : AbstractJsIrTextTestBase<ClassicFrontendOutputArtifact>() {

    override val frontend: FrontendKind<*>
        get() = FrontendKinds.ClassicFrontend

    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val converter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter

    override val klibFacades: KlibFacades
        get() = KlibFacades(
            serializerFacade = ::ClassicJsKlibSerializerFacade,
            deserializerFacade = ::JsIrDeserializerFacade,
        )
}

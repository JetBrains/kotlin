/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import org.jetbrains.kotlin.js.test.converters.JsKlibBackendFacade
import org.jetbrains.kotlin.js.test.handlers.JsCollectAndMemorizeIdSignatures
import org.jetbrains.kotlin.js.test.handlers.JsVerifyIdSignaturesByDeserializedIr
import org.jetbrains.kotlin.js.test.handlers.JsVerifyIdSignaturesByK1LazyIr
import org.jetbrains.kotlin.js.test.handlers.JsVerifyIdSignaturesByK2LazyIr
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.AbstractVerifyIdSignaturesByK2LazyIr
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.ir.AbstractIrTextTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

abstract class AbstractJsIrTextTestBase<FrontendOutput> :
    AbstractIrTextTest<FrontendOutput, BinaryArtifacts.KLib>(
        JsPlatforms.defaultJsPlatform,
        TargetBackend.JS_IR
    ) where FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput> {

    final override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsEnvironmentConfigurator,
        )

        useAdditionalService(::LibraryProvider)
    }
}

open class AbstractClassicJsIrTextTest : AbstractJsIrTextTestBase<ClassicFrontendOutputArtifact>() {

    final override val klibSignatureVerification = KlibSignatureVerification(
        collectAndMemorizeIdSignatures = ::JsCollectAndMemorizeIdSignatures,
        verifySignaturesByDeserializedIr = ::JsVerifyIdSignaturesByDeserializedIr,
        verifySignaturesByK1LazyIr = ::JsVerifyIdSignaturesByK1LazyIr,
        verifySignaturesByK2LazyIr = ::JsVerifyIdSignaturesByK2LazyIr,
        backendFacade = ::JsKlibBackendFacade
    )

    override val frontend: FrontendKind<*>
        get() = FrontendKinds.ClassicFrontend

    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val converter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter
}

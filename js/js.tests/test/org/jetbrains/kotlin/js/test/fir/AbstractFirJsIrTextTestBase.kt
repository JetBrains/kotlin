/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWebFacade
import org.jetbrains.kotlin.js.test.converters.JsIrDeserializerFacade
import org.jetbrains.kotlin.js.test.ir.AbstractJsIrTextTestBase
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.handlers.AbstractKlibAbiDumpBeforeInliningSavingHandler
import org.jetbrains.kotlin.test.backend.handlers.FirJsKlibAbiDumpBeforeInliningSavingHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.KlibFacades
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.additionalK2ConfigurationForIrTextTest
import org.jetbrains.kotlin.test.directives.KlibAbiConsistencyDirectives.CHECK_SAME_ABI_AFTER_INLINING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.codegen.FirPsiCodegenTest

abstract class AbstractFirJsIrTextTestBase(private val parser: FirParser) : AbstractJsIrTextTestBase<FirOutputArtifact>() {

    override val frontend: FrontendKind<*>
        get() = FrontendKinds.FIR

    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirCliWebFacade

    override val converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrCliWebFacade

    override val klibAbiDumpBeforeInliningSavingHandler: Constructor<AbstractKlibAbiDumpBeforeInliningSavingHandler>?
        get() = ::FirJsKlibAbiDumpBeforeInliningSavingHandler

    override val klibFacades: KlibFacades
        get() = KlibFacades(
            serializerFacade = ::FirKlibSerializerCliWebFacade,
            deserializerFacade = ::JsIrDeserializerFacade,
        )

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.additionalK2ConfigurationForIrTextTest(parser)
        with(builder) {
            defaultDirectives {
                +CHECK_SAME_ABI_AFTER_INLINING
                LANGUAGE with "+${LanguageFeature.IrInlinerBeforeKlibSerialization.name}"
            }
        }
    }
}

open class AbstractFirLightTreeJsIrTextTest : AbstractFirJsIrTextTestBase(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiJsIrTextTest : AbstractFirJsIrTextTestBase(FirParser.Psi)

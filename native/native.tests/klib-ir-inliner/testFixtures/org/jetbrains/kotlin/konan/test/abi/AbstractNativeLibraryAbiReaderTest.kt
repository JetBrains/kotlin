/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.abi

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.NativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.converters.NativePreSerializationLoweringFacade
import org.jetbrains.kotlin.library.abi.AbstractLibraryAbiReaderTest
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.model.BackendFacade
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.IrPreSerializationLoweringFacade
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.junit.jupiter.api.Tag

@Tag("klib")
open class AbstractNativeLibraryAbiReaderTest :
    AbstractLibraryAbiReaderTest<FirOutputArtifact>(NativePlatforms.unspecifiedNativePlatform, TargetBackend.NATIVE) {

    final override val frontend: FrontendKind<*>
        get() = FrontendKinds.FIR

    final override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override val converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrNativeResultsConverter

    override val preserializerFacade: Constructor<IrPreSerializationLoweringFacade<IrBackendInput>>
        get() = ::NativePreSerializationLoweringFacade

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::NativeKlibSerializerFacade

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        configureFirParser(FirParser.LightTree)
        defaultDirectives {
            LANGUAGE with listOf(
                "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
            )
            // Kotlin/Native does not have "minimal" stdlib(like other backends do), so full stdlib is needed to resolve
            // `Any`, `String`, `println`, etc.
            +ConfigurationDirectives.WITH_STDLIB
        }
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::NativeEnvironmentConfigurator,
        )
        super.configure(builder)
    }
}

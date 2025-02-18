/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.abi

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.test.ClassicFrontend2NativeIrConverter
import org.jetbrains.kotlin.konan.test.ClassicNativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.FirNativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.converters.NativeInliningFacade
import org.jetbrains.kotlin.library.abi.AbstractLibraryAbiReaderTest
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.model.BackendFacade
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.IrInliningFacade
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.junit.jupiter.api.Tag

@Tag("klib")
abstract class AbstractNativeLibraryAbiReaderTest<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>> :
    AbstractLibraryAbiReaderTest<FrontendOutput>(NativePlatforms.unspecifiedNativePlatform, TargetBackend.NATIVE) {

    override val preserializerFacade: Constructor<IrInliningFacade<IrBackendInput>>
        get() = ::NativeInliningFacade

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
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

open class AbstractFirNativeLibraryAbiReaderTest : AbstractNativeLibraryAbiReaderTest<FirOutputArtifact>() {
    final override val frontend: FrontendKind<*>
        get() = FrontendKinds.FIR

    final override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override val converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrNativeResultsConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::FirNativeKlibSerializerFacade

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        configureFirParser(FirParser.LightTree)
        defaultDirectives {
            LANGUAGE with "+${LanguageFeature.IrInlinerBeforeKlibSerialization.name}"
        }

        super.configure(builder)
    }
}

open class AbstractClassicNativeLibraryAbiReaderTest : AbstractNativeLibraryAbiReaderTest<ClassicFrontendOutputArtifact>() {
    final override val frontend: FrontendKind<*>
        get() = FrontendKinds.ClassicFrontend

    final override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val converter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2NativeIrConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::ClassicNativeKlibSerializerFacade
}

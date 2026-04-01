/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.abi

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.test.Fir2IrCliNativeFacade
import org.jetbrains.kotlin.konan.test.FirCliNativeFacade
import org.jetbrains.kotlin.konan.test.KlibSerializerNativeCliFacade
import org.jetbrains.kotlin.konan.test.NativePreSerializationLoweringCliFacade
import org.jetbrains.kotlin.library.abi.AbstractLibraryAbiReaderTest
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator
import org.junit.jupiter.api.Tag

@Tag("klib")
open class AbstractNativeLibraryAbiReaderTest :
    AbstractLibraryAbiReaderTest(NativePlatforms.unspecifiedNativePlatform, TargetBackend.NATIVE) {

    final override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirCliNativeFacade

    override val converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrCliNativeFacade

    override val preserializerFacade: Constructor<IrPreSerializationLoweringFacade<IrBackendInput>>
        get() = ::NativePreSerializationLoweringCliFacade

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::KlibSerializerNativeCliFacade

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
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
            ::NativeFirstStageEnvironmentConfigurator,
        )
        super.configure(builder)
    }
}

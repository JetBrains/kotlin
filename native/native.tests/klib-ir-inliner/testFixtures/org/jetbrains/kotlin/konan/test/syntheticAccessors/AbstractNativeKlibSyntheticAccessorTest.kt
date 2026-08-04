/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.syntheticAccessors

import org.jetbrains.kotlin.config.PartialLinkageLogLevel
import org.jetbrains.kotlin.konan.test.Fir2IrCliNativeFacade
import org.jetbrains.kotlin.konan.test.FirCliNativeFacade
import org.jetbrains.kotlin.konan.test.KlibSerializerNativeCliFacade
import org.jetbrains.kotlin.konan.test.NativePreSerializationLoweringCliFacade
import org.jetbrains.kotlin.konan.test.converters.NativeDeserializerFacade
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.commonConfigurationForDumpSyntheticAccessorsTest
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind

// Base class for IR dump synthetic accessors test, configured with FIR frontend, in Native-specific way.
open class AbstractNativeKlibSyntheticAccessorTest(
    // Use the ERROR log level by default to fail any tests where PL detected any incompatibilities.
    private val partialLinkageLogLevel: PartialLinkageLogLevel = PartialLinkageLogLevel.ERROR
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.NATIVE) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForDumpSyntheticAccessorsTest(
            frontendFacade = ::FirCliNativeFacade,
            frontendToIrConverter = ::Fir2IrCliNativeFacade,
            irInliningFacade = ::NativePreSerializationLoweringCliFacade,
            serializerFacade = ::KlibSerializerNativeCliFacade,
            deserializerFacade = ::NativeDeserializerFacade.bind(partialLinkageLogLevel),
        )
        globalDefaults {
            targetPlatform = NativePlatforms.unspecifiedNativePlatform
        }
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::NativeFirstStageEnvironmentConfigurator,
        )
    }
}

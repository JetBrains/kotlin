/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners

import org.jetbrains.kotlin.js.test.converters.*
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.commonConfigurationForDumpSyntheticAccessorsTest
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.JsFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsSecondStageEnvironmentConfigurator

abstract class AbstractJsKlibSyntheticAccessorTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JS_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForDumpSyntheticAccessorsTest(
            frontendFacade = ::FirCliWebFacade,
            frontendToIrConverter = ::Fir2IrCliWebFacade,
            irInliningFacade = ::JsIrPreSerializationLoweringFacade,
            serializerFacade = ::FirKlibSerializerCliJsFacade,
            deserializerFacade = ::JsIrDeserializerFacade,
        )
        globalDefaults {
            targetPlatform = JsPlatforms.defaultJsPlatform
        }
        useConfigurators(
            ::JsFirstStageEnvironmentConfigurator,
            ::JsSecondStageEnvironmentConfigurator,
        )
    }
}

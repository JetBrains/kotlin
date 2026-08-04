/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWasmFacade
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.commonConfigurationForDumpSyntheticAccessorsTest
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.WasmFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlin.wasm.test.converters.WasmDeserializerFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmPreSerializationLoweringFacade

open class AbstractWasmJsKlibSyntheticAccessorTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.WASM) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForDumpSyntheticAccessorsTest(
            frontendFacade = ::FirCliWebFacade,
            frontendToIrConverter = ::Fir2IrCliWebFacade,
            irInliningFacade = ::WasmPreSerializationLoweringFacade,
            serializerFacade = ::FirKlibSerializerCliWasmFacade,
            deserializerFacade = ::WasmDeserializerFacade,
        )
        globalDefaults {
            targetPlatform = WasmPlatforms.wasmJs
        }
        useConfigurators(
            ::WasmFirstStageEnvironmentConfigurator.bind(WasmTarget.JS),
            ::WasmSecondStageEnvironmentConfigurator.bind(WasmTarget.JS),
        )
    }
}


/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrPreSerializationWasmSymbolValidationHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.klib.AbstractPreSerializationSymbolsTest
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind

@Suppress("JUnitTestCaseWithNoTests")
class WasmPreSerializationSymbolsTest : AbstractPreSerializationSymbolsTest(
    TargetBackend.WASM,
    WasmPlatforms.unspecifiedWasmPlatform,
    ::FirFrontendFacade,
    ::Fir2IrResultsConverter,
    ::IrPreSerializationWasmSymbolValidationHandler,
) {
    override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(::WasmEnvironmentConfigurator.bind(WasmTarget.JS))
    }
}

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.backend.common.ir.PreSerializationSymbols
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.BackendJsSymbols
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWebFacade
import org.jetbrains.kotlin.js.test.converters.JsIrDeserializerFacade
import org.jetbrains.kotlin.js.test.converters.JsIrPreSerializationLoweringFacade
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrPreSerializationJsSymbolValidationHandler
import org.jetbrains.kotlin.test.backend.ir.IrSecondPhaseSymbolValidationHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.klib.AbstractSymbolsValidationTest
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsSecondStageEnvironmentConfigurator

@Suppress("JUnitTestCaseWithNoTests")
class JsSymbolsTest : AbstractSymbolsValidationTest(
    TargetBackend.JS_IR,
    JsPlatforms.defaultJsPlatform,
    ::FirCliWebFacade,
    ::Fir2IrCliWebFacade,
    ::JsIrPreSerializationLoweringFacade,
    ::FirKlibSerializerCliWebFacade,
    ::JsIrDeserializerFacade,
    ::IrPreSerializationJsSymbolValidationHandler,
    ::JsSymbolValidationHandler,
) {
    override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(
            ::JsFirstStageEnvironmentConfigurator,
            ::JsSecondStageEnvironmentConfigurator,
        )
    }
}

private class JsSymbolValidationHandler(testServices: TestServices) : IrSecondPhaseSymbolValidationHandler(testServices) {
    override fun getSymbols(irBuiltIns: IrBuiltIns): List<PreSerializationSymbols> {
        return listOf(
            BackendJsSymbols(irBuiltIns, StageController(), compileLongAsBigint = true),
            BackendJsSymbols(irBuiltIns, StageController(), compileLongAsBigint = false),
        )
    }
}

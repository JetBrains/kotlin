/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrPreSerializationJsSymbolValidationHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.klib.AbstractPreSerializationSymbolsTest
import org.jetbrains.kotlin.test.services.configuration.JsFirstStageEnvironmentConfigurator

@Suppress("JUnitTestCaseWithNoTests")
class JsPreSerializationSymbolsTest : AbstractPreSerializationSymbolsTest(
    TargetBackend.JS_IR,
    JsPlatforms.defaultJsPlatform,
    ::FirCliWebFacade,
    ::Fir2IrCliWebFacade,
    ::IrPreSerializationJsSymbolValidationHandler
) {
    override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(::JsFirstStageEnvironmentConfigurator)
    }
}

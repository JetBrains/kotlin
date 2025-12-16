/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.NativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.converters.NativeDeserializerFacade
import org.jetbrains.kotlin.konan.test.converters.NativePreSerializationLoweringFacade
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrPreSerializationNativeSymbolValidationHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.klib.AbstractSymbolsValidationTest
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator

@Suppress("JUnitTestCaseWithNoTests")
class NativeSymbolsTest : AbstractSymbolsValidationTest(
    TargetBackend.NATIVE,
    NativePlatforms.unspecifiedNativePlatform,
    ::FirFrontendFacade,
    ::Fir2IrNativeResultsConverter,
    ::NativePreSerializationLoweringFacade,
    ::NativeKlibSerializerFacade,
    ::NativeDeserializerFacade,
    ::IrPreSerializationNativeSymbolValidationHandler,
) {
    override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(::NativeEnvironmentConfigurator)
    }
}

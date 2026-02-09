/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.common.ir.PreSerializationSymbols
import org.jetbrains.kotlin.backend.konan.ir.BackendNativeSymbols
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.konan.test.Fir2IrCliNativeFacade
import org.jetbrains.kotlin.konan.test.FirCliNativeFacade
import org.jetbrains.kotlin.konan.test.KlibSerializerNativeCliFacade
import org.jetbrains.kotlin.konan.test.NativePreSerializationLoweringCliFacade
import org.jetbrains.kotlin.konan.test.converters.NativeDeserializerFacade
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrPreSerializationNativeSymbolValidationHandler
import org.jetbrains.kotlin.test.backend.ir.IrSecondPhaseSymbolValidationHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.klib.AbstractSymbolsValidationTest
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator

@Suppress("JUnitTestCaseWithNoTests")
class NativeSymbolsTest : AbstractSymbolsValidationTest(
    TargetBackend.NATIVE,
    NativePlatforms.unspecifiedNativePlatform,
    ::FirCliNativeFacade,
    ::Fir2IrCliNativeFacade,
    ::NativePreSerializationLoweringCliFacade,
    ::KlibSerializerNativeCliFacade,
    ::NativeDeserializerFacade,
    ::IrPreSerializationNativeSymbolValidationHandler,
    ::NativeSymbolValidationHandler,
) {
    override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(::NativeFirstStageEnvironmentConfigurator)
    }
}

class NativeSymbolValidationHandler(testServices: TestServices) : IrSecondPhaseSymbolValidationHandler(testServices) {
    private val errorReportingContext = object : ErrorReportingContext {
        override val messageCollector: MessageCollector
            get() = error("should not be called")
    }

    override fun getSymbols(irBuiltIns: IrBuiltIns): List<PreSerializationSymbols> {
        return listOf(BackendNativeSymbols(errorReportingContext, irBuiltIns, CompilerConfiguration.create()))
    }
}

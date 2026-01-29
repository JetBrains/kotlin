/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.backend.common.ir.PreSerializationSymbols
import org.jetbrains.kotlin.backend.wasm.BackendWasmSymbols
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrPreSerializationWasmSymbolValidationHandler
import org.jetbrains.kotlin.test.backend.ir.IrSecondPhaseSymbolValidationHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.klib.AbstractSymbolsValidationTest
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.test.converters.FirWasmKlibSerializerFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmDeserializerFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmPreSerializationLoweringFacade

@Suppress("JUnitTestCaseWithNoTests")
class WasmJsSymbolsTest : AbstractSymbolsValidationTest(
    TargetBackend.WASM_JS,
    WasmPlatforms.unspecifiedWasmPlatform,
    ::FirFrontendFacade,
    ::Fir2IrResultsConverter,
    ::WasmPreSerializationLoweringFacade,
    ::FirWasmKlibSerializerFacade,
    ::WasmDeserializerFacade,
    ::IrPreSerializationWasmSymbolValidationHandler,
    ::WasmJsSymbolValidationHandler,
) {
    override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(
            ::WasmFirstStageEnvironmentConfigurator.bind(WasmTarget.JS),
            ::WasmSecondStageEnvironmentConfigurator.bind(WasmTarget.JS),
        )
    }
}

class WasmJsSymbolValidationHandler(testServices: TestServices) : IrSecondPhaseSymbolValidationHandler(testServices) {
    override val blackList: List<String> = listOf("createDynamicKType", "invokeOnExportedFunctionExit")

    override fun getSymbols(irBuiltIns: IrBuiltIns): List<PreSerializationSymbols> {
        val configurationJs = CompilerConfiguration.create().apply { put(WasmConfigurationKeys.WASM_TARGET, WasmTarget.JS) }
        return listOf(BackendWasmSymbols(irBuiltIns, configurationJs))
    }
}

@Suppress("JUnitTestCaseWithNoTests")
class WasmWasiSymbolsTest : AbstractSymbolsValidationTest(
    TargetBackend.WASM_WASI,
    WasmPlatforms.wasmWasi,
    ::FirFrontendFacade,
    ::Fir2IrResultsConverter,
    ::WasmPreSerializationLoweringFacade,
    ::FirWasmKlibSerializerFacade,
    ::WasmDeserializerFacade,
    ::IrPreSerializationWasmSymbolValidationHandler,
    ::WasmWasiSymbolValidationHandler,
) {
    override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(
            ::WasmFirstStageEnvironmentConfigurator.bind(WasmTarget.WASI),
            ::WasmSecondStageEnvironmentConfigurator.bind(WasmTarget.WASI),
        )
    }
}

class WasmWasiSymbolValidationHandler(testServices: TestServices) : IrSecondPhaseSymbolValidationHandler(testServices) {
    override val blackList: List<String> = listOf("createDynamicKType", "jsRelatedSymbols")

    override fun getSymbols(irBuiltIns: IrBuiltIns): List<PreSerializationSymbols> {
        val configurationWasi = CompilerConfiguration.create().apply { put(WasmConfigurationKeys.WASM_TARGET, WasmTarget.WASI) }
        return listOf(BackendWasmSymbols(irBuiltIns, configurationWasi))
    }
}


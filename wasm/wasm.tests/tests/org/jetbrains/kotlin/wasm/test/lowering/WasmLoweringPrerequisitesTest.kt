/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.lowering

import org.jetbrains.kotlin.backend.common.LoweringPrerequisitesTest
import org.jetbrains.kotlin.backend.wasm.wasmLowerings
import org.jetbrains.kotlin.backend.wasm.wasmLoweringsOfTheFirstPhase
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.junit.jupiter.api.Test

class WasmLoweringPrerequisitesTest : LoweringPrerequisitesTest() {
    @Test
    fun checkPrerequisites() {
        val settings = LanguageVersionSettingsImpl(
            LanguageVersion.LATEST_STABLE,
            ApiVersion.LATEST_STABLE,
            specificFeatures = mapOf(
                LanguageFeature.IrRichCallableReferencesInKlibs to LanguageFeature.State.ENABLED,
                LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization to LanguageFeature.State.ENABLED,
                LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization to LanguageFeature.State.ENABLED,
            )
        )
        checkPrerequisites(wasmLoweringsOfTheFirstPhase(settings))
        checkPrerequisites(wasmLowerings)
    }
}
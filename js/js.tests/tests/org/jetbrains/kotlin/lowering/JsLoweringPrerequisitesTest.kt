/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lowering

import org.jetbrains.kotlin.backend.common.LoweringPrerequisitesTest
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.ir.backend.js.getJsLowerings
import org.jetbrains.kotlin.ir.backend.js.jsLoweringsOfTheFirstPhase
import org.jetbrains.kotlin.ir.backend.js.optimizationLoweringList
import org.junit.jupiter.api.Test

class JsLoweringPrerequisitesTest : LoweringPrerequisitesTest() {
    @Test
    fun checkPrerequisites() {
        val settings = LanguageVersionSettingsImpl(
            LanguageVersion.Companion.LATEST_STABLE,
            ApiVersion.Companion.LATEST_STABLE,
            specificFeatures = mapOf(
                LanguageFeature.IrRichCallableReferencesInKlibs to LanguageFeature.State.ENABLED,
                LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization to LanguageFeature.State.ENABLED,
                LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization to LanguageFeature.State.ENABLED,
            )
        )
        checkPrerequisites(jsLoweringsOfTheFirstPhase(settings))
        checkPrerequisites(getJsLowerings())
        checkPrerequisites(optimizationLoweringList)
    }
}
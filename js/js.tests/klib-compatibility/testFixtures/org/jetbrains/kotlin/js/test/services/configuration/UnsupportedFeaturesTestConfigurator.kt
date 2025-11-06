/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.services.configuration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.moduleStructure

class UnsupportedFeaturesTestConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        val testModule = testServices.moduleStructure.modules.first()
        val languageVersion = testServices.compilerConfigurationProvider.getCompilerConfiguration(
            testModule,
            CompilationStage.FIRST
        ).languageVersionSettings.languageVersion

        listOf(
            LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization,
            LanguageFeature.NameBasedDestructuring,
            LanguageFeature.ContextParameters,
        ).forEach {
            if (it.isNeededButNotSupported(testModule, languageVersion)) {
                return true
            }
        }
        return false
    }

    private fun LanguageFeature.isSupportedInLV(languageVersion: LanguageVersion): Boolean =
        sinceVersion?.let {
            it.major >= languageVersion.major || it.minor >= languageVersion.minor
        } ?: false

    private fun LanguageFeature.isNeededInModule(testModule: TestModule): Boolean =
        testModule.languageVersionSettings.supportsFeature(this)

    private fun LanguageFeature.isNeededButNotSupported(testModule: TestModule, languageVersion: LanguageVersion): Boolean =
        isNeededInModule(testModule) && !isSupportedInLV(languageVersion)
}


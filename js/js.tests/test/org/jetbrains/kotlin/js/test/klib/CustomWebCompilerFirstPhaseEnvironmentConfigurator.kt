/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.enableIrVisibilityChecks
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Disable IR visibility checks on the second compilation phase for KLIBs produced by older compiler versions.
 * - 2.1.0-Beta2: All [IrField]s on non-JVM backends must be private (KT-71232).
 *
 * Note: This configuration is supposed to be used in tests with the custom compiler on the first compilation phase.
 */
class CustomWebCompilerFirstPhaseEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (customJsCompilerSettings.defaultLanguageVersion < LanguageVersion.KOTLIN_2_1) {
            configuration.enableIrVisibilityChecks = false
        }
    }
}

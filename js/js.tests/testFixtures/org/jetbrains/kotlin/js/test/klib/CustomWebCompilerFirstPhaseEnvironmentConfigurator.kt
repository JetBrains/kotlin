/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.config.verifyIr
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Disable IR verification on the second compilation phase for KLIBs produced by older compiler versions.
 *
 * Note: This configuration is supposed to be used in tests with the custom compiler on the first compilation phase.
 */
class CustomWebCompilerFirstPhaseEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.verifyIr = IrVerificationMode.NONE
    }
}

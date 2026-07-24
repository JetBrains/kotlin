/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.finalizePath
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

class JsSizeHandler(testServices: TestServices) : AbstractJsArtifactsCollector(testServices) {
    companion object {
        private const val THRESHOLD_PERCENT = 2
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (someAssertionWasFailed) return
        checkExpectedDceOutputSize()
    }

    private fun checkExpectedDceOutputSize() {
        val (expectedOutputSize) = testServices.moduleStructure.allDirectives[JsEnvironmentConfigurationDirectives.JS_DCE_EXPECTED_OUTPUT_SIZE]
            .firstOrNull { it.forTargetBackend == null || it.forTargetBackend == testServices.defaultsProvider.targetBackend } ?: return

        val mainModule = JsEnvironmentConfigurator.getMainModule(testServices)
        val moduleKind = JsEnvironmentConfigurator.getModuleKind(testServices, mainModule)

        val outputFile = File(
            JsEnvironmentConfigurator
                .getJsModuleArtifactPath(testServices, mainModule.name, TranslationMode.FULL_PROD_MINIMIZED_NAMES)
                .finalizePath(moduleKind)
        )
        val actualSize = outputFile.length().toInt()

        val thresholdInBytes = expectedOutputSize / 100 * THRESHOLD_PERCENT
        val expectedMinSize = expectedOutputSize - thresholdInBytes
        val expectedMaxSize = expectedOutputSize + thresholdInBytes
        val diff = actualSize - expectedOutputSize

        if (actualSize !in expectedMinSize..expectedMaxSize) {
            throw AssertionError(
                "Size of ${outputFile.name} is ${actualSize.toFormattedString()}," +
                        " but expected $expectedOutputSize ∓ $thresholdInBytes [$expectedMinSize .. $expectedMaxSize]." +
                        " Diff: $diff (${diff * 100 / expectedOutputSize}%)"
            )
        }
    }
}

private fun Int.toFormattedString(): String {
    return this.toString().reversed().chunked(3).joinToString("_").reversed()
}

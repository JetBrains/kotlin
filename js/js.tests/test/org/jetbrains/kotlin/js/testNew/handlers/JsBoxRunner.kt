/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testNew.handlers

import org.jetbrains.kotlin.js.testNew.utils.*
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure

class JsBoxRunner(testServices: TestServices) : AbstractJsArtifactsCollector(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (someAssertionWasFailed) return

        val globalDirectives = testServices.moduleStructure.allDirectives
        val dontRunGeneratedCode = globalDirectives[JsEnvironmentConfigurationDirectives.DONT_RUN_GENERATED_CODE]
            .contains(testServices.defaultsProvider.defaultTargetBackend?.name)

        if (dontRunGeneratedCode) return

        val (allJsFiles, dceAllJsFiles, pirAllJsFiles) = getAllFilesForRunner(testServices, modulesToArtifact)

        val withModuleSystem = testWithModuleSystem(testServices)
        val testModuleName = getTestModuleName(testServices)
        val testPackage = extractTestPackage(testServices)

        val dontSkipRegularMode = JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE !in globalDirectives
        val dontSkipDceDriven = JsEnvironmentConfigurationDirectives.SKIP_DCE_DRIVEN !in globalDirectives
        val runIrDce = JsEnvironmentConfigurationDirectives.RUN_IR_DCE in globalDirectives
        val runIrPir = JsEnvironmentConfigurationDirectives.RUN_IR_PIR in globalDirectives
        if (dontSkipRegularMode) {
            runGeneratedCode(allJsFiles, testModuleName, testPackage, withModuleSystem)

            if (runIrDce) {
                runGeneratedCode(dceAllJsFiles, testModuleName, testPackage, withModuleSystem)
            }
        }

        if (runIrPir && dontSkipDceDriven) {
            runGeneratedCode(pirAllJsFiles, testModuleName, testPackage, withModuleSystem)
        }
    }

    private fun runGeneratedCode(jsFiles: List<String>, testModuleName: String?, testPackage: String?, withModuleSystem: Boolean) {
        getTestChecker(testServices)
            .check(jsFiles, testModuleName, testPackage, TEST_FUNCTION, DEFAULT_EXPECTED_RESULT, withModuleSystem)
    }

    companion object {
        const val DEFAULT_EXPECTED_RESULT = "OK"
        const val TEST_FUNCTION = "box"
    }
}
/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.js.test.utils.*
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure

class JsBoxRunner(testServices: TestServices) : AbstractJsArtifactsCollector(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!someAssertionWasFailed) {
            runJsCode()
        }
    }

    private fun runJsCode() {
        val globalDirectives = testServices.moduleStructure.allDirectives
        val dontRunGeneratedCode = globalDirectives[JsEnvironmentConfigurationDirectives.DONT_RUN_GENERATED_CODE]
            .contains(testServices.defaultsProvider.defaultTargetBackend?.name)

        if (dontRunGeneratedCode) return

        val allJsFiles = getAllFilesForRunner(testServices, modulesToArtifact)

        val withModuleSystem = testWithModuleSystem(testServices)
        val testModuleName = getTestModuleName(testServices)
        val testPackage = extractTestPackage(testServices)

        val dontSkipRegularMode = JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE !in globalDirectives
        if (dontSkipRegularMode) {
            for ((mode, jsFiles) in allJsFiles) {
                val entryModulePath = extractEntryModulePath(mode, testServices)
                runGeneratedCode(entryModulePath, jsFiles, testModuleName, testPackage, withModuleSystem)
            }
        }
    }

    private fun runGeneratedCode(
        entryModulePath: String?,
        jsFiles: List<String>,
        testModuleName: String?,
        testPackage: String?,
        withModuleSystem: Boolean
    ) {
        getTestChecker(testServices)
            .check(
                jsFiles,
                testModuleName,
                testPackage,
                TEST_FUNCTION,
                DEFAULT_EXPECTED_RESULT,
                withModuleSystem,
                entryModulePath,
            )
    }

    companion object {
        const val DEFAULT_EXPECTED_RESULT = "OK"
        const val TEST_FUNCTION = "box"
    }
}
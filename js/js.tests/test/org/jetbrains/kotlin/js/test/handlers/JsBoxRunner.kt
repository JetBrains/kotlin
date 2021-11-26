/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.js.testOld.engines.ExternalTool
import org.jetbrains.kotlin.js.test.utils.*
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

private val v8tool by lazy { ExternalTool(System.getProperty("javascript.engine.path.V8")) }

class JsBoxRunner(testServices: TestServices) : AbstractJsArtifactsCollector(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (someAssertionWasFailed) return

        if (JsEnvironmentConfigurationDirectives.ES_MODULES in testServices.moduleStructure.allDirectives) {
            runEsCode()
        } else {
            runJsCode()
        }
    }

    private fun runJsCode() {
        val globalDirectives = testServices.moduleStructure.allDirectives
        val dontRunGeneratedCode = globalDirectives[JsEnvironmentConfigurationDirectives.DONT_RUN_GENERATED_CODE]
            .contains(testServices.defaultsProvider.defaultTargetBackend?.name)

        if (dontRunGeneratedCode) return

        val (allJsFiles, dceAllJsFiles) = getAllFilesForRunner(testServices, modulesToArtifact)

        val withModuleSystem = testWithModuleSystem(testServices)
        val testModuleName = getTestModuleName(testServices)
        val testPackage = extractTestPackage(testServices)

        val dontSkipRegularMode = JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE !in globalDirectives
        val dontSkipIrIc = JsEnvironmentConfigurationDirectives.SKIP_IR_INCREMENTAL_CHECKS !in globalDirectives
        val recompile = testServices.moduleStructure.modules
            .flatMap { it.files }.any { JsEnvironmentConfigurationDirectives.RECOMPILE in it.directives }
        val runIrDce = JsEnvironmentConfigurationDirectives.RUN_IR_DCE in globalDirectives
        if (dontSkipRegularMode) {
            runGeneratedCode(allJsFiles, testModuleName, testPackage, withModuleSystem)

            if (runIrDce && !(dontSkipIrIc && recompile)) {
                runGeneratedCode(dceAllJsFiles, testModuleName, testPackage, withModuleSystem)
            }
        }
    }

    private fun runEsCode() {
        val globalDirectives = testServices.moduleStructure.allDirectives

        val esmOutputDir = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices).esModulesSubDir
        val esmDceOutputDir = JsEnvironmentConfigurator.getDceJsArtifactsOutputDir(testServices).esModulesSubDir

        val dontSkipRegularMode = JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE !in globalDirectives
        val runIrDce = JsEnvironmentConfigurationDirectives.RUN_IR_DCE in globalDirectives
        if (dontSkipRegularMode) {
            singleRunEsCode(esmOutputDir)
            if (runIrDce) {
                singleRunEsCode(esmDceOutputDir)
            }
        }
    }

    private fun singleRunEsCode(esmOutputDir: File) {
        val perFileEsModuleFile = "$esmOutputDir/test.mjs"
        val (allNonEsModuleFiles, inputJsFilesAfter) = extractAllFilesForEsRunner(testServices, esmOutputDir)
        v8tool.run(*allNonEsModuleFiles.toTypedArray(), perFileEsModuleFile, *inputJsFilesAfter.toTypedArray())
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
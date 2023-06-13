/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.test.utils.getTestChecker
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

class JsWrongModuleHandler(testServices: TestServices) : JsBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {}

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val originalFileName = testServices.moduleStructure.originalTestDataFiles.first().nameWithoutExtension
        val parentDir = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, translationMode = TranslationMode.PER_MODULE_DEV)
        val kotlinJsFile = File(parentDir, "$originalFileName-kotlin_kotlin_v5.js").path
        val mainJsFile = File(parentDir, "${originalFileName}_v5.js").path
        val libJsFile = File(parentDir, "$originalFileName-kotlin_lib_v5.js").path
        try {
            getTestChecker(testServices).run(listOf(kotlinJsFile, mainJsFile, libJsFile))
        } catch (e: RuntimeException) {
            testServices.assertions.assertTrue(e is IllegalStateException)
            val message = e.message!!

            testServices.assertions.assertTrue("'kotlin_lib'" in message) {
                "Exception message should contain reference to dependency (lib)"
            }
            testServices.assertions.assertTrue("'main'" in message) {
                "Exception message should contain reference to module that failed to load (main)"
            }
            return
        }
        testServices.assertions.fail { "Exception should have been thrown due to wrong order of modules" }
    }
}
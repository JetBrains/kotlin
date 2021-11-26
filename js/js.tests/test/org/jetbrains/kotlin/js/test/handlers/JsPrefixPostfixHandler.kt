/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

class JsPrefixPostfixHandler(testServices: TestServices) : JsBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {
        val outputPrefixFile = JsEnvironmentConfigurator.getPrefixFile(module)
        val outputPostfixFile = JsEnvironmentConfigurator.getPostfixFile(module)

        val outputText = info.outputFile.readText()
        outputPrefixFile?.let { testServices.assertions.assertTrue(outputText.startsWith(it.readText())) }
        outputPostfixFile?.let { testServices.assertions.assertTrue(outputText.endsWith(it.readText())) }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
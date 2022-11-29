/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull

class JsDtsHandler(testServices: TestServices) : JsBinaryArtifactHandler(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {
        val globalDirectives = testServices.moduleStructure.allDirectives
        if (JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE in globalDirectives) return

        val referenceDtsFile = module.files.first().originalFile.withReplacedExtensionOrNull(".kt", ".d.ts")
            ?: error("Can't find reference .d.ts file")
        val generatedDtsFile = info.outputFile.withReplacedExtensionOrNull("_v5.js", ".d.ts")
            ?: info.outputFile.withReplacedExtensionOrNull("_v5.mjs", ".d.ts")
            ?: error("Can't find generated .d.ts file")

        val generatedDts = generatedDtsFile.readText()

        if (JsEnvironmentConfigurationDirectives.UPDATE_REFERENCE_DTS_FILES in globalDirectives)
            referenceDtsFile.writeText(generatedDts)
        else
            KotlinTestUtils.assertEqualsToFile(referenceDtsFile, generatedDts)
    }
}

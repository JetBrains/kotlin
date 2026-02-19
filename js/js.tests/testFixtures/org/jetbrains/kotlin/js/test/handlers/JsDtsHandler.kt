/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull

class JsDtsHandler(testServices: TestServices, private val expectedDtsSuffix: String? = null) : JsBinaryArtifactHandler(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {
        val globalDirectives = testServices.moduleStructure.allDirectives
        if (JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE in globalDirectives) return

        // TODO: fix the issue with difference in name of the file and the generated file
        val suffix = if (expectedDtsSuffix != null) ".$expectedDtsSuffix" else ""
        val extension = if (JsEnvironmentConfigurationDirectives.ES_MODULES in globalDirectives) "-lib_v5$suffix.d.mts" else "$suffix.d.ts"

        val referenceDtsFile = module.files.first().originalFile.withReplacedExtensionOrNull(".kt", extension)
            ?: error("Can't find reference $extension file")

        val generatedDtsFile = info.dtsFile
            ?: error("Can't find generated .d.ts file")

        val generatedDts = generatedDtsFile.readText()
        assertions.assertEqualsToFile(referenceDtsFile, generatedDts)
    }
}

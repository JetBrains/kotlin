/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test.integration

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.kapt3.test.KaptContextBinaryArtifact
import org.jetbrains.kotlin.kapt3.test.handlers.AbstractKaptHandler
import org.jetbrains.kotlin.kapt3.test.handlers.checkTxtAccordingToBackend
import org.jetbrains.kotlin.kapt3.test.handlers.removeMetadataAnnotationContents
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF

class KaptIntegrationStubsDumpHandler(testServices: TestServices) : AbstractKaptHandler(testServices) {
    companion object {
        private const val FILE_SUFFIX = ".it"
    }

    override fun processModule(module: TestModule, info: KaptContextBinaryArtifact) {
        val actualRaw = testServices.kapt3ExtensionProvider[module].savedStubs ?: assertions.fail { "Stubs were not saved" }
        val actual = StringUtil.convertLineSeparators(actualRaw.trim { it <= ' ' })
            .trimTrailingWhitespacesAndAddNewlineAtEOF()
            .let { removeMetadataAnnotationContents(it) }
        assertions.checkTxtAccordingToBackend(module, actual, FILE_SUFFIX)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}

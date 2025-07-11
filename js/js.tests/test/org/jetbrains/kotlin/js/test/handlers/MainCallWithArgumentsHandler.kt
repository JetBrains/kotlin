/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.js.test.utils.getOnlyJsFilesForRunner
import org.jetbrains.kotlin.js.testOld.V8JsTestChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

class MainCallWithArgumentsHandler(testServices: TestServices) : AbstractJsArtifactsCollector(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (someAssertionWasFailed) return

        val testFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile = testFile.parent.resolve(testFile.nameWithoutExtension + ".out")
        if (!expectedFile.exists()) {
            throw IllegalArgumentException("File ${expectedFile.toAbsolutePath()} with expected result doesn't exists")
        }
        val allJsFiles = getOnlyJsFilesForRunner(testServices, modulesToArtifact)

        V8JsTestChecker.checkStdout(allJsFiles, expectedFile.readText())
    }
}
/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.js.test.utils.getOnlyJsFilesForRunner
import org.jetbrains.kotlin.js.test.utils.getTestChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class MainCallWithArgumentsHandler(testServices: TestServices) : AbstractJsArtifactsCollector(testServices) {
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (someAssertionWasFailed) return

        val testFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile = testFile.parentFile.resolve(testFile.nameWithoutExtension + ".out")
        if (!expectedFile.exists()) {
            throw IllegalArgumentException("File ${expectedFile.absolutePath} with expected result doesn't exists")
        }
        val allJsFiles = getOnlyJsFilesForRunner(testServices, modulesToArtifact)

        getTestChecker(testServices).checkStdout(allJsFiles, expectedFile.readText())
    }
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testNew

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.backend.classic.ClassicBackendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.firTestDataFile

class JsFailingTestSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val testFile = testServices.moduleStructure.originalTestDataFiles.first()
        val failFile = testFile.parentFile.resolve("${testFile.name}.fail").takeIf { it.exists() }
            ?: return failedAssertions
        if (failedAssertions.any { it is WrappedException.FromFacade }) return emptyList()
        return failedAssertions + AssertionError("Fail file exists but no exception was thrown. Please remove ${failFile.name}").wrap()
    }
}

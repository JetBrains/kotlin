/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.junit.jupiter.api.Assumptions

class JsFailingTestSuppressor(
    testServices: TestServices,
    private val suppressWithAssumption: Boolean = false
) : AfterAnalysisChecker(testServices) {
    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val testFile = testServices.moduleStructure.originalTestDataFiles.first()
        val failFile = testFile.parentFile.resolve("${testFile.name}.fail").takeIf { it.exists() } ?: return failedAssertions

        if (failedAssertions.any { it is WrappedException.FromFacade }) {
            if (suppressWithAssumption)
                Assumptions.abort<Nothing>()
            else
                return emptyList()
        }

        return failedAssertions + AssertionError("Fail file exists but no exception was thrown. Please remove ${failFile.name}").wrap()
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class WasmFailingTestSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val testFile = testServices.moduleStructure.originalTestDataFiles.first()
        val failFile = testFile.parentFile.resolve("${testFile.name}.fail").takeIf { it.exists() }
            ?: return failedAssertions
        if (failedAssertions.any { it is WrappedException.FromFacade }) return emptyList()
        return failedAssertions + AssertionError("Fail file exists but no exception was thrown. Please remove ${failFile.name}").wrap()
    }
}

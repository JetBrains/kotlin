/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.tests

import org.jetbrains.kotlin.abi.tools.AbiFilters
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import kotlin.io.path.ExperimentalPathApi

class PrecompiledCasesTest {

    companion object {
        val baseOutputPath = File("src/sharedTests/resources/precompiled")
    }

    private lateinit var testMethodName: String

    @BeforeEach
    fun captureTestName(testInfo: TestInfo) {
        testMethodName = testInfo.testMethod.get().name
    }

    @Test
    fun parcelable() = snapshotAPIAndCompare()

    @Test
    fun jar() {
        val testDir = baseOutputPath.resolve(testMethodName)
        val target = testDir.resolve(testMethodName + ".txt")

        doCheck(listOf(testDir), target, AbiFilters.EMPTY)
    }

    @OptIn(ExperimentalPathApi::class)
    private fun snapshotAPIAndCompare(
        includedClasses: Set<String> = emptySet(),
        excludedClasses: Set<String> = emptySet(),
        includedAnnotatedWith: Set<String> = emptySet(),
        excludedAnnotatedWith: Set<String> = emptySet(),
    ) {
        val testDir = baseOutputPath.resolve(testMethodName)
        val filters = AbiFilters(includedClasses, excludedClasses, includedAnnotatedWith, excludedAnnotatedWith)
        val target = testDir.resolve(testMethodName + ".txt")

        doCheck(listOf(testDir), target, filters)
    }
}

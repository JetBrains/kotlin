/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.tests

import org.jetbrains.kotlin.abi.tools.AbiFilters
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File

class PrecompiledCasesTest {

    companion object {
        val baseOutputPath = File("src/sharedTests/resources/precompiled")
    }

    @Test
    fun parcelable(testInfo: TestInfo) = snapshotAPIAndCompare(testInfo)

    @Test
    fun jar(testInfo: TestInfo) {
        val methodName = testInfo.testMethod.get().name
        val testDir = baseOutputPath.resolve(methodName)
        val target = testDir.resolve("$methodName.txt")

        doCheck(listOf(testDir), target, AbiFilters.EMPTY)
    }

    private fun snapshotAPIAndCompare(
        testInfo: TestInfo,
        includedClasses: Set<String> = emptySet(),
        excludedClasses: Set<String> = emptySet(),
        includedAnnotatedWith: Set<String> = emptySet(),
        excludedAnnotatedWith: Set<String> = emptySet(),
    ) {
        val methodName = testInfo.testMethod.get().name
        val testDir = baseOutputPath.resolve(methodName)
        val filters = AbiFilters(includedClasses, excludedClasses, includedAnnotatedWith, excludedAnnotatedWith)
        val target = testDir.resolve("$methodName.txt")

        doCheck(listOf(testDir), target, filters)
    }
}

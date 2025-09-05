/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.impl

import org.jetbrains.kotlin.abi.tools.AbiFilters
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File
import kotlin.io.path.ExperimentalPathApi

class PrecompiledCasesTest {

    companion object {
        val baseOutputPath = File("src/test/resources/precompiled")
    }

    @Rule
    @JvmField
    val testName = TestName()

    @Test
    fun parcelable() = snapshotAPIAndCompare()

    @Test
    fun jar() {
        val testDir = baseOutputPath.resolve(testName.methodName)
        val target = testDir.resolve(testName.methodName + ".txt")

        doCheck(listOf(testDir), target, AbiFilters.EMPTY)
    }

    @OptIn(ExperimentalPathApi::class)
    private fun snapshotAPIAndCompare(
        includedClasses: Set<String> = emptySet(),
        excludedClasses: Set<String> = emptySet(),
        includedAnnotatedWith: Set<String> = emptySet(),
        excludedAnnotatedWith: Set<String> = emptySet(),
    ) {
        val testDir = baseOutputPath.resolve(testName.methodName)
        val filters = AbiFilters(includedClasses, excludedClasses, includedAnnotatedWith, excludedAnnotatedWith)
        val target = testDir.resolve(testName.methodName + ".txt")

        doCheck(listOf(testDir), target, filters)
    }
}

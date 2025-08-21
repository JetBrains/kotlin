/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.legacy

import org.jetbrains.kotlin.abi.tools.api.AbiFilters
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

    @OptIn(ExperimentalPathApi::class)
    private fun snapshotAPIAndCompare(
        includedClasses: Set<String> = emptySet(),
        excludedClasses: Set<String> = emptySet(),
        includedAnnotatedWith: Set<String> = emptySet(),
        excludedAnnotatedWith: Set<String> = emptySet(),
    ) {
        val testClassRelativePath = testName.methodName
        val filters = AbiFilters(includedClasses, excludedClasses, includedAnnotatedWith, excludedAnnotatedWith)

        val target = baseOutputPath.resolve(testClassRelativePath).resolve(testName.methodName + ".txt")

        doCheck(listOf(baseOutputPath), target, filters)
    }
}

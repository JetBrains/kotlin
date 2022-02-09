/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.konan.blackboxtest.support.TestCaseId
import org.jetbrains.kotlin.konan.blackboxtest.support.TestRunnerType
import org.jetbrains.kotlin.konan.blackboxtest.support.group.PredefinedPaths.KOTLIN_NATIVE_DISTRIBUTION
import org.jetbrains.kotlin.konan.blackboxtest.support.group.PredefinedTestCase as TC
import org.jetbrains.kotlin.konan.blackboxtest.support.group.PredefinedTestCases
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestFactory

@Tag("codegen")
@PredefinedTestCases(
    TC(
        name = "nativeStdlib",
        runnerType = TestRunnerType.DEFAULT,
        freeCompilerArgs = [ENABLE_MPP, STDLIB_IS_A_FRIEND, ENABLE_X_STDLIB_API],
        sourceLocations = [
            "libraries/stdlib/test/**.kt",
            "libraries/stdlib/common/test/**.kt",
            "kotlin-native/backend.native/tests/stdlib_external/text/**.kt",
            "kotlin-native/backend.native/tests/stdlib_external/utils.kt",
            "kotlin-native/backend.native/tests/stdlib_external/jsCollectionFactoriesActuals.kt"
        ],
        ignoredTests = [DISABLED_STDLIB_TEST]
    ),
    TC(
        name = "nativeStdlibInWorker",
        runnerType = TestRunnerType.WORKER,
        freeCompilerArgs = [ENABLE_MPP, STDLIB_IS_A_FRIEND, ENABLE_X_STDLIB_API],
        sourceLocations = [
            "libraries/stdlib/test/**.kt",
            "libraries/stdlib/common/test/**.kt",
            "kotlin-native/backend.native/tests/stdlib_external/text/**.kt",
            "kotlin-native/backend.native/tests/stdlib_external/utils.kt",
            "kotlin-native/backend.native/tests/stdlib_external/jsCollectionFactoriesActuals.kt"
        ],
        ignoredTests = [DISABLED_STDLIB_TEST]
    ),
    TC(
        name = "kotlinTest",
        runnerType = TestRunnerType.DEFAULT,
        freeCompilerArgs = [STDLIB_IS_A_FRIEND],
        sourceLocations = ["libraries/kotlin.test/common/src/test/kotlin/**.kt"]
    ),
    TC(
        name = "kotlinTestInWorker",
        runnerType = TestRunnerType.WORKER,
        freeCompilerArgs = [STDLIB_IS_A_FRIEND],
        sourceLocations = ["libraries/kotlin.test/common/src/test/kotlin/**.kt"]
    )
)
class StdlibTest : AbstractNativeBlackBoxTest() {
    @TestFactory
    fun kotlinTest() = dynamicTestCase(TestCaseId.Named("kotlinTest"))

    @TestFactory
    fun kotlinTestInWorker() = dynamicTestCase(TestCaseId.Named("kotlinTestInWorker"))

    @TestFactory
    fun nativeStdlib() = dynamicTestCase(TestCaseId.Named("nativeStdlib"))

    @TestFactory
    fun nativeStdlibInWorker() = dynamicTestCase(TestCaseId.Named("nativeStdlibInWorker"))
}

private const val ENABLE_MPP = "-Xmulti-platform"
private const val STDLIB_IS_A_FRIEND = "-friend-modules=$KOTLIN_NATIVE_DISTRIBUTION/klib/common/stdlib"
private const val ENABLE_X_STDLIB_API = "-opt-in=kotlin.RequiresOptIn,kotlin.ExperimentalStdlibApi"
private const val DISABLED_STDLIB_TEST = "test.collections.CollectionTest.abstractCollectionToArray"
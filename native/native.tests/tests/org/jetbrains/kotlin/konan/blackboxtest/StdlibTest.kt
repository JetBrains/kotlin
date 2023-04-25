/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.konan.blackboxtest.support.TestCaseId
import org.jetbrains.kotlin.konan.blackboxtest.support.TestRunnerType
import org.jetbrains.kotlin.konan.blackboxtest.support.group.FirPipeline
import org.jetbrains.kotlin.konan.blackboxtest.support.group.PredefinedPaths.KOTLIN_NATIVE_DISTRIBUTION
import org.jetbrains.kotlin.konan.blackboxtest.support.group.PredefinedTestCase as TC
import org.jetbrains.kotlin.konan.blackboxtest.support.group.PredefinedTestCases
import org.jetbrains.kotlin.konan.blackboxtest.support.group.UsePartialLinkage
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestFactory

@Tag("stdlib")
@PredefinedTestCases(
    TC(
        name = "default",
        runnerType = TestRunnerType.DEFAULT,
        freeCompilerArgs = [ENABLE_MPP, STDLIB_IS_A_FRIEND, ENABLE_X_STDLIB_API, ENABLE_X_ENCODING_API, ENABLE_RANGE_UNTIL],
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
        name = "worker",
        runnerType = TestRunnerType.WORKER,
        freeCompilerArgs = [ENABLE_MPP, STDLIB_IS_A_FRIEND, ENABLE_X_STDLIB_API, ENABLE_X_ENCODING_API, ENABLE_RANGE_UNTIL],
        sourceLocations = [
            "libraries/stdlib/test/**.kt",
            "libraries/stdlib/common/test/**.kt",
            "kotlin-native/backend.native/tests/stdlib_external/text/**.kt",
            "kotlin-native/backend.native/tests/stdlib_external/utils.kt",
            "kotlin-native/backend.native/tests/stdlib_external/jsCollectionFactoriesActuals.kt"
        ],
        ignoredTests = [DISABLED_STDLIB_TEST]
    )
)
@UsePartialLinkage(UsePartialLinkage.Mode.DISABLED)
class StdlibTest : AbstractNativeBlackBoxTest() {
    @TestFactory
    fun default() = dynamicTestCase(TestCaseId.Named("default"))

    @TestFactory
    fun worker() = dynamicTestCase(TestCaseId.Named("worker"))
}

@Tag("stdlibK2")
@Tag("firStdlib")
@PredefinedTestCases(
    TC(
        name = "default",
        runnerType = TestRunnerType.DEFAULT,
        freeCompilerArgs = [ENABLE_MPP, STDLIB_IS_A_FRIEND, ENABLE_X_STDLIB_API, ENABLE_X_ENCODING_API, ENABLE_RANGE_UNTIL,
            "-Xcommon-sources=libraries/stdlib/common/test/jsCollectionFactories.kt",
            "-Xcommon-sources=libraries/stdlib/common/test/testUtils.kt",
            "-Xcommon-sources=libraries/stdlib/test/testUtils.kt",
            "-Xcommon-sources=libraries/stdlib/test/text/StringEncodingTest.kt",
        ],
        sourceLocations = [
            "libraries/stdlib/test/**.kt",
            "libraries/stdlib/common/test/**.kt",
            "kotlin-native/backend.native/tests/stdlib_external/text/**.kt",
            "kotlin-native/backend.native/tests/stdlib_external/utils.kt",
            "kotlin-native/backend.native/tests/stdlib_external/jsCollectionFactoriesActuals.kt"
        ],
        ignoredFiles = [
            DISABLED_K2_ARRAYS,
        ],
        ignoredTests = [DISABLED_STDLIB_TEST]
    ),
    TC(
        name = "worker",
        runnerType = TestRunnerType.WORKER,
        freeCompilerArgs = [ENABLE_MPP, STDLIB_IS_A_FRIEND, ENABLE_X_STDLIB_API, ENABLE_X_ENCODING_API, ENABLE_RANGE_UNTIL,
            "-Xcommon-sources=libraries/stdlib/common/test/jsCollectionFactories.kt",
            "-Xcommon-sources=libraries/stdlib/common/test/testUtils.kt",
            "-Xcommon-sources=libraries/stdlib/test/testUtils.kt",
            "-Xcommon-sources=libraries/stdlib/test/text/StringEncodingTest.kt",
        ],
        sourceLocations = [
            "libraries/stdlib/test/**.kt",
            "libraries/stdlib/common/test/**.kt",
            "kotlin-native/backend.native/tests/stdlib_external/text/**.kt",
            "kotlin-native/backend.native/tests/stdlib_external/utils.kt",
            "kotlin-native/backend.native/tests/stdlib_external/jsCollectionFactoriesActuals.kt"
        ],
        ignoredFiles = [
            DISABLED_K2_ARRAYS,
        ],
        ignoredTests = [DISABLED_STDLIB_TEST]
    )
)
@FirPipeline
@UsePartialLinkage(UsePartialLinkage.Mode.DISABLED)
class FirStdlibTest : AbstractNativeBlackBoxTest() {
    @TestFactory
    fun default() = dynamicTestCase(TestCaseId.Named("default"))

    @TestFactory
    fun worker() = dynamicTestCase(TestCaseId.Named("worker"))
}

private const val ENABLE_MPP = "-Xmulti-platform"
internal const val STDLIB_IS_A_FRIEND = "-friend-modules=$KOTLIN_NATIVE_DISTRIBUTION/klib/common/stdlib"
private const val ENABLE_X_STDLIB_API = "-opt-in=kotlin.ExperimentalStdlibApi"
private const val ENABLE_X_ENCODING_API = "-opt-in=kotlin.io.encoding.ExperimentalEncodingApi"
private const val ENABLE_RANGE_UNTIL = "-XXLanguage:+RangeUntilOperator" // keep until 1.8
private const val DISABLED_STDLIB_TEST = "test.collections.CollectionTest.abstractCollectionToArray"

private const val DISABLED_K2_ARRAYS = "libraries/stdlib/test/collections/ArraysTest.kt"  // KT-57487

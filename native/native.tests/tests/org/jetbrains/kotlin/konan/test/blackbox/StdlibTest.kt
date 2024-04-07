/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseId
import org.jetbrains.kotlin.konan.test.blackbox.support.TestRunnerType
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.group.PredefinedPaths.KOTLIN_NATIVE_DISTRIBUTION
import org.jetbrains.kotlin.konan.test.blackbox.support.group.PredefinedTestCases
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UsePartialLinkage
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.group.PredefinedTestCase as TC

@Tag("stdlib")
@PredefinedTestCases(
    TC(
        name = "default",
        runnerType = TestRunnerType.DEFAULT,
        freeCompilerArgs = [
            ENABLE_MPP, STDLIB_IS_A_FRIEND, ENABLE_X_STDLIB_API, ENABLE_X_ENCODING_API, ENABLE_RANGE_UNTIL,
            ENABLE_X_FOREIGN_API, ENABLE_X_NATIVE_API, ENABLE_OBSOLETE_NATIVE_API, ENABLE_NATIVE_RUNTIME_API,
            ENABLE_OBSOLETE_WORKERS_API, ENABLE_INTERNAL_FOR_KOTLIN_NATIVE,
            "-language-version", "1.9",
            "-api-version", "2.0",
            "-Xsuppress-api-version-greater-than-language-version-error"
        ],
        sourceLocations = [
            "libraries/stdlib/test/**.kt",
            "libraries/stdlib/common/test/**.kt",
            "libraries/stdlib/native-wasm/test/**.kt",
            "kotlin-native/runtime/test/**.kt"
        ],
        ignoredTests = [DISABLED_STDLIB_TEST]
    )
)
@EnforcedProperty(property = ClassLevelProperty.EXECUTION_TIMEOUT, propertyValue = "2m")
@UsePartialLinkage(UsePartialLinkage.Mode.DISABLED)
class StdlibTest : AbstractNativeBlackBoxTest() {
    @TestFactory
    fun default() = dynamicTestCase(TestCaseId.Named("default"))
}

@Tag("stdlib")
@Tag("frontend-fir")
@PredefinedTestCases(
    TC(
        name = "default",
        runnerType = TestRunnerType.DEFAULT,
        freeCompilerArgs = [
            ENABLE_MPP, STDLIB_IS_A_FRIEND, ENABLE_X_STDLIB_API, ENABLE_X_ENCODING_API, ENABLE_RANGE_UNTIL,
            ENABLE_X_FOREIGN_API, ENABLE_X_NATIVE_API, ENABLE_OBSOLETE_NATIVE_API, ENABLE_NATIVE_RUNTIME_API,
            ENABLE_OBSOLETE_WORKERS_API, ENABLE_INTERNAL_FOR_KOTLIN_NATIVE,
            "-Xcommon-sources=libraries/stdlib/common/test/jsCollectionFactories.kt",
            "-Xcommon-sources=libraries/stdlib/common/test/testUtils.kt",
            "-Xcommon-sources=libraries/stdlib/test/testUtils.kt",
            "-Xcommon-sources=libraries/stdlib/test/text/StringEncodingTest.kt",
        ],
        sourceLocations = [
            "libraries/stdlib/test/**.kt",
            "libraries/stdlib/common/test/**.kt",
            "libraries/stdlib/native-wasm/test/**.kt",
            "kotlin-native/runtime/test/**.kt"
        ],
        ignoredTests = [DISABLED_STDLIB_TEST]
    )
)
@EnforcedProperty(property = ClassLevelProperty.EXECUTION_TIMEOUT, propertyValue = "2m")
@FirPipeline
@UsePartialLinkage(UsePartialLinkage.Mode.DISABLED)
class FirStdlibTest : AbstractNativeBlackBoxTest() {
    @TestFactory
    fun default() = dynamicTestCase(TestCaseId.Named("default"))
}

private const val ENABLE_MPP = "-Xmulti-platform"
internal const val STDLIB_IS_A_FRIEND = "-friend-modules=$KOTLIN_NATIVE_DISTRIBUTION/klib/common/stdlib"
private const val ENABLE_X_STDLIB_API = "-opt-in=kotlin.ExperimentalStdlibApi"
private const val ENABLE_X_ENCODING_API = "-opt-in=kotlin.io.encoding.ExperimentalEncodingApi"
private const val ENABLE_X_FOREIGN_API = "-opt-in=kotlinx.cinterop.ExperimentalForeignApi"
private const val ENABLE_X_NATIVE_API = "-opt-in=kotlin.experimental.ExperimentalNativeApi"
private const val ENABLE_OBSOLETE_NATIVE_API = "-opt-in=kotlin.native.ObsoleteNativeApi"
private const val ENABLE_NATIVE_RUNTIME_API = "-opt-in=kotlin.native.runtime.NativeRuntimeApi"
private const val ENABLE_OBSOLETE_WORKERS_API = "-opt-in=kotlin.native.concurrent.ObsoleteWorkersApi"
private const val ENABLE_INTERNAL_FOR_KOTLIN_NATIVE = "-opt-in=kotlin.native.internal.InternalForKotlinNative"
private const val ENABLE_RANGE_UNTIL = "-XXLanguage:+RangeUntilOperator" // keep until 1.8
private const val DISABLED_STDLIB_TEST = "test.collections.CollectionTest.abstractCollectionToArray"
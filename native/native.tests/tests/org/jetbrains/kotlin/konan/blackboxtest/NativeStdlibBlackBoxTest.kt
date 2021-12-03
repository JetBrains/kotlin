/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.konan.blackboxtest.support.TestCaseId
import org.jetbrains.kotlin.konan.blackboxtest.support.group.PredefinedTestCase as TC
import org.jetbrains.kotlin.konan.blackboxtest.support.group.PredefinedTestCases
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestFactory

@Tag("daily")
@PredefinedTestCases(
    TC(
        name = "nativeStdlib",
        freeCompilerArgs = [
            "-Xmulti-platform",
            "-friend-modules=$KOTLIN_NATIVE_STDLIB_PATH",
            "-opt-in=kotlin.RequiresOptIn,kotlin.ExperimentalStdlibApi"
        ],
        sourceLocations = [
            "libraries/stdlib/test/**.kt",
            "libraries/stdlib/common/test/**.kt",
            "kotlin-native/backend.native/tests/stdlib_external/text/**.kt",
            "kotlin-native/backend.native/tests/stdlib_external/utils.kt",
            "kotlin-native/backend.native/tests/stdlib_external/jsCollectionFactoriesActuals.kt"
        ]
    ),
    TC(
        name = "kotlinTest",
        freeCompilerArgs = ["-friend-modules=$KOTLIN_NATIVE_STDLIB_PATH"],
        sourceLocations = ["libraries/kotlin.test/common/src/test/kotlin/**.kt"]
    )
)
class NativeStdlibBlackBoxTest : AbstractNativeBlackBoxTest() {
    @TestFactory
    fun kotlinTest() = dynamicTestCase(TestCaseId.Named("kotlinTest"))

    @TestFactory
    fun nativeStdlib() = dynamicTestCase(TestCaseId.Named("nativeStdlib"))
}

private const val KOTLIN_NATIVE_STDLIB_PATH = "kotlin-native/dist/klib/common/stdlib"

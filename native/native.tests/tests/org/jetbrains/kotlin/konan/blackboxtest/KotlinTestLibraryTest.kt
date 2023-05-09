/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.konan.blackboxtest.support.TestCaseId
import org.jetbrains.kotlin.konan.blackboxtest.support.TestRunnerType
import org.jetbrains.kotlin.konan.blackboxtest.support.group.FirPipeline
import org.jetbrains.kotlin.konan.blackboxtest.support.group.PredefinedTestCases
import org.jetbrains.kotlin.konan.blackboxtest.support.group.UsePartialLinkage
import org.jetbrains.kotlin.konan.blackboxtest.support.group.PredefinedTestCase as TC
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestFactory

@Tag("kotlin-test")
@PredefinedTestCases(
    TC(
        name = "default",
        runnerType = TestRunnerType.DEFAULT,
        freeCompilerArgs = [STDLIB_IS_A_FRIEND],
        sourceLocations = ["libraries/kotlin.test/common/src/test/kotlin/**.kt"]
    ),
    TC(
        name = "worker",
        runnerType = TestRunnerType.WORKER,
        freeCompilerArgs = [STDLIB_IS_A_FRIEND],
        sourceLocations = ["libraries/kotlin.test/common/src/test/kotlin/**.kt"]
    )
)
@UsePartialLinkage(UsePartialLinkage.Mode.DISABLED)
class KotlinTestLibraryTest : AbstractNativeBlackBoxTest() {
    @TestFactory
    fun default() = dynamicTestCase(TestCaseId.Named("default"))

    @TestFactory
    fun worker() = dynamicTestCase(TestCaseId.Named("worker"))
}

@Tag("kotlin-test")
@Tag("frontend-fir")
@PredefinedTestCases(
    TC(
        name = "default",
        runnerType = TestRunnerType.DEFAULT,
        freeCompilerArgs = [STDLIB_IS_A_FRIEND],
        sourceLocations = ["libraries/kotlin.test/common/src/test/kotlin/**.kt"]
    ),
    TC(
        name = "worker",
        runnerType = TestRunnerType.WORKER,
        freeCompilerArgs = [STDLIB_IS_A_FRIEND],
        sourceLocations = ["libraries/kotlin.test/common/src/test/kotlin/**.kt"]
    )
)
@FirPipeline
@UsePartialLinkage(UsePartialLinkage.Mode.DISABLED)
class FirKotlinTestLibraryTest : AbstractNativeBlackBoxTest() {
    @TestFactory
    fun default() = dynamicTestCase(TestCaseId.Named("default"))

    @TestFactory
    fun worker() = dynamicTestCase(TestCaseId.Named("worker"))
}

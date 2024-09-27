/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseId
import org.jetbrains.kotlin.konan.test.blackbox.support.TestRunnerType
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.group.PredefinedTestCases
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UsePartialLinkage
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.group.PredefinedTestCase as TC

@Tag("kotlin-ui-test")
@Tag("frontend-fir")
@PredefinedTestCases(
    TC(
        name = "HostAppTestTests",
        runnerType = TestRunnerType.DEFAULT,
        freeCompilerArgs = [STDLIB_IS_A_FRIEND],
        sourceLocations = ["native/native.tests/testData/UIApp/**.kt"]
    )
)
@FirPipeline
@UsePartialLinkage(UsePartialLinkage.Mode.DISABLED)
class KotlinUIApplicationTest : AbstractNativeBlackBoxTest() {
    @TestFactory
    fun default() = dynamicTestCase(TestCaseId.Named("HostAppTestTests"))
}

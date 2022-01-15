/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.group

import org.jetbrains.kotlin.konan.blackboxtest.support.TestRunnerType
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.TestConfiguration

@Target(AnnotationTarget.CLASS)
@TestConfiguration(providerClass = PredefinedTestCaseGroupProvider::class)
internal annotation class PredefinedTestCases(vararg val testCases: PredefinedTestCase)

@Target()
internal annotation class PredefinedTestCase(
    val name: String,
    val runnerType: TestRunnerType,
    val freeCompilerArgs: Array<String>,
    val sourceLocations: Array<String>,
    val ignoredTests: Array<String> = []
)

internal object PredefinedPaths {
    const val KOTLIN_NATIVE_DISTRIBUTION = "\$KOTLIN_NATIVE_DISTRIBUTION\$"
}

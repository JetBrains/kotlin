/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.group

import org.jetbrains.kotlin.konan.test.blackbox.support.TestRunnerType
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestConfiguration

@Target(AnnotationTarget.CLASS)
@TestConfiguration(providerClass = PredefinedTestCaseGroupProvider::class)
internal annotation class PredefinedTestCases(vararg val testCases: PredefinedTestCase)

@Target()
internal annotation class PredefinedTestCase(
    val name: String,
    val runnerType: TestRunnerType,
    val freeCompilerArgs: Array<String>,
    val sourceLocations: Array<String>,
    val ignoredFiles: Array<String> = [],  // TODO Remove it after fix of KT-55902, KT-56023, KT-56483
    val ignoredTests: Array<String> = []
)

internal object PredefinedPaths {
    const val KOTLIN_NATIVE_DISTRIBUTION = "\$KOTLIN_NATIVE_DISTRIBUTION\$"

    /**
     * Substitutes predefined path placeholders in [value] with actual paths from [settings].
     * Currently, supports only [KOTLIN_NATIVE_DISTRIBUTION] placeholder.
     */
    fun substitutePlaceholders(value: String, settings: Settings): String =
        if ('$' in value) {
            // N.B. Here, more substitutions can be supported in the future if it would be necessary.
            value.replace(KOTLIN_NATIVE_DISTRIBUTION, settings.get<KotlinNativeHome>().dir.path)
        } else {
            value
        }
}

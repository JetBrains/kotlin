/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import org.jetbrains.kotlin.gradle.util.isTeamCityRun
import org.jetbrains.kotlin.test.util.convertLineSeparators
import org.jetbrains.kotlin.test.util.trimTrailingWhitespaces
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.fail

internal fun BuildResult.printBuildOutput() {
    println(failedAssertionOutput())
}
internal fun BuildResult.failedAssertionOutput() = """
        |Failed assertion build output:
        |#######################
        |$output
        |#######################
        |
        """.trimMargin()

internal fun String.normalizeLineEndings(): String = replace("\n", System.lineSeparator())

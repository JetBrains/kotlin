/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing

import java.io.File

fun generateFixtureIfMissing(expected: File, actual: File): File {
    if (expected.exists()) return expected
    val isTeamcityRun = System.getenv("TEAMCITY_VERSION") != null
    if (isTeamcityRun) error("Generating fixture at ${expected} during CI execution is not permitted.")

    if (!actual.exists()) {
        error("Missing the expected fixture at ${expected} and actual file at ${actual}")
    }
    if (!expected.parentFile.exists()) {
        error("Please create parent directory in ${expected.parentFile}")
    }
    actual.copyRecursively(expected, overwrite = false)
    error("Copied fixture from ${actual} into ${expected}. Rerun the test")
}
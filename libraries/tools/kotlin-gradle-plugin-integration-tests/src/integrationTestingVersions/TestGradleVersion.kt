/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

// https://gradle.org/nightly/
// Gradle nightly releases retention policy is 3 months
object TestGradleVersion {
    val all = listOf(
        G_8_13,
        G_8_14,
        G_9_0,
        G_9_1,
        G_9_2,
        G_9_3,
        G_9_4,
        G_9_5,
        G_9_6,
        G_9_7,
        G_9_8,
    )

    const val G_8_13 = "8.13"
    const val G_8_14 = "8.14.5"
    const val G_9_0 = "9.0.0"
    const val G_9_1 = "9.1.0"
    const val G_9_2 = "9.2.1"
    const val G_9_3 = "9.3.1"
    const val G_9_4 = "9.4.1"
    const val G_9_5 = "9.5.1"
    const val G_9_6 = "9.6.1"
    const val G_9_7 = "9.7.0"
    const val G_9_8 = "9.8.1"

    /**
     * Check [org.jetbrains.kotlin.gradle.GradleCompatibilityIT.testIncompatibleGradleVersion]
     */
    const val MIN_UNSUPPORTED_VERSION_TO_CHECK = G_8_13

    // Should be the same minor release as GradleCompatibilityCheck.minSupportedGradleVersion
    // Check [org.jetbrains.kotlin.gradle.GradleTestVersionsSanityTest]
    const val MIN_SUPPORTED = G_8_14
    const val MAX_SUPPORTED = G_9_8
}

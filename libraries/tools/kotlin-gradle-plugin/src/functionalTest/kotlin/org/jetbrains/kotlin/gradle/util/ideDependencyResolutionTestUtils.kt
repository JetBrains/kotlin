/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates

/**
 * Legacy -jdk8 and -jdk7 dependencies:
 * Those artifacts will be published as empty jars starting from Kotlin 1.8.0 as
 * the classes will be included in the kotlin-stdlib artifact already.
 *
 * Note: The kotlin-stdlib will add a constraint to always resolve 1.8.0 of those artifacts.
 * This will be necessary in the future, when no more jdk8 or jdk7 artifacts will be published:
 * In this case we need to still resolve to a version that will contain empty artifacts (1.8.0)
 *
 */
fun legacyStdlibJdkDependencies(version: String = "1.8.0") = listOf(
    binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"),
    binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$version"),
)

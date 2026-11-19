/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

/** Shadows Gradle's factory in expressions while preserving its type in test signatures. */
@Suppress("PropertyName")
val GradleVersion = GradleVersionFactory

object GradleVersionFactory {
    fun version(version: TestVersions.Gradle): org.gradle.util.GradleVersion = version(version.version)

    fun version(version: String): org.gradle.util.GradleVersion = org.gradle.util.GradleVersion.version(version)
}

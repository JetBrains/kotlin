/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName

class DependencyLockingIT : KGPBaseTest() {
    @JvmGradlePluginTests
    @DisplayName("KT-71549: dependency locking does not cause build failure")
    @TestMetadata("jvm-with-dependency-locking")
    @GradleTest
    @Disabled("It's flaky. See the issue comments, first requires a proper fix. Additionally, on Windows it sometimes fails due to inability to delete the tmp directory.")
    fun testJvmDependencyLocking(gradleVersion: GradleVersion) {
        project("jvm-with-dependency-locking", gradleVersion) {
            build("compileKotlin", "--write-locks")
            build("compileKotlin")
        }
    }
}
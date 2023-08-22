/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

class ProjectIsolationIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(configurationCache = true, projectIsolation = true)

    @DisplayName("JVM project should be compatible with project isolation")
    @JvmGradlePluginTests
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_7_1,
    )
    @GradleTest
    fun testProjectIsolationInJvmSimple(gradleVersion: GradleVersion) {
        project(
            projectName = "instantExecution",
            gradleVersion = gradleVersion,
        ) {
            build(":main-project:compileKotlin")
        }
    }
}

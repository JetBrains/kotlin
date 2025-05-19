/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.BrokenOnMacosTest
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@JsGradlePluginTests
class CleanDataTaskIT : KGPBaseTest() {

    @DisplayName("nodejs is deleted from Gradle user home")
    @GradleTest
    fun testDownloadedFolderDeletion(gradleVersion: GradleVersion) {
        project(
            "cleanTask",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.copy(isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED),
        ) {
            build("testCleanTask")
        }
    }
}

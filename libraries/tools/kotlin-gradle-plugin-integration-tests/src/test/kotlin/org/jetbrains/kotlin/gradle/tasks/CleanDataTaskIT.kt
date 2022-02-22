/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
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
            buildOptions = defaultBuildOptions.copy(
                // bug in Gradle: https://github.com/gradle/gradle/issues/15796
                warningMode = if (gradleVersion < GradleVersion.version("7.0")) WarningMode.Summary else defaultBuildOptions.warningMode
            )
        ) {
            build("testCleanTask")
        }
    }
}

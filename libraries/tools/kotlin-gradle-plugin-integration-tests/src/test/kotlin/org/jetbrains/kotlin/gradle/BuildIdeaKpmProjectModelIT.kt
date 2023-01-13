/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@GradleTestVersions(minVersion = TestVersions.Gradle.MIN_SUPPORTED_KPM)
class BuildIdeaKpmProjectModelIT : KGPBaseTest() {

    @GradleTest
    @DisplayName("Check 'buildIdeaKpmProjectModel' creates expected files")
    fun `test - simple kpm project`(gradleVersion: GradleVersion) {
        project(
            "kpm-simple", gradleVersion, buildOptions = defaultBuildOptions.copy(
                // Workaround for KT-55751
                warningMode = WarningMode.None,
            )
        ) {
            build("buildIdeaKpmProjectModel") {
                assertFileInProjectExists("build/IdeaKpmProject/model.txt")
                assertFileInProjectExists("build/IdeaKpmProject/model.java.bin")
                assertFileInProjectExists("build/IdeaKpmProject/model.proto.bin")
            }
        }
    }
}

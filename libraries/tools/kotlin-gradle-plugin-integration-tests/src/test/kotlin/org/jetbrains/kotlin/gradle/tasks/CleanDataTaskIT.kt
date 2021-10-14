/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.junit.Test
import org.junit.jupiter.api.DisplayName


@SimpleGradlePluginTests
class CleanDataTaskIT : KGPBaseTest() {

    @DisplayName("nodejs is deleted from Gradle user home")
    @GradleTest
    fun testDownloadedFolderDeletion(gradleVersion: GradleVersion) {
        project("cleanTask", gradleVersion) {
            build("testCleanTask")
        }
    }
}

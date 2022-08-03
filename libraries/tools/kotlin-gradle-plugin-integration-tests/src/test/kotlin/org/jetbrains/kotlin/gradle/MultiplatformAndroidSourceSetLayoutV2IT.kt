/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("Multiplatform Build Reproducibility")
class MultiplatformAndroidSourceSetLayoutV2IT : KGPBaseTest() {

    @GradleAndroidTest
    @AndroidTestVersions(minVersion = "7.0.4")
    fun testProjectWithFlavors(gradleVersion: GradleVersion, agpVersion: String, jdkVersion: JdkVersions.ProvidedJdk) {
        val buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion)
    }
}
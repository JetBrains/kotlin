/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.forceKapt4
import org.jetbrains.kotlin.gradle.testbase.JdkVersions
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName

@DisplayName("android with kapt4 incremental build tests")
class Kapt4AndroidIncrementalIT : Kapt3AndroidIncrementalIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()

    override fun TestProject.customizeProject() {
        forceKapt4()
    }

    @Disabled("See KT-61628")
    override fun testAndroidDaggerIC(gradleVersion: GradleVersion, agpVersion: String, jdkVersion: JdkVersions.ProvidedJdk) {}
}

@DisplayName("android with kapt4 incremental build tests with precise compilation outputs backup")
class Kapt4AndroidIncrementalWithPreciseBackupIT : Kapt3AndroidIncrementalWithPreciseBackupIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()

    override fun TestProject.customizeProject() {
        forceKapt4()
    }

    @Disabled("See KT-61628")
    override fun testAndroidDaggerIC(gradleVersion: GradleVersion, agpVersion: String, jdkVersion: JdkVersions.ProvidedJdk) {}
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

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
            // we can remove this line, when the min version of Gradle be at least 8.1
            dependencyManagement = DependencyManagement.DisabledDependencyManagement
        ) {
            build(":main-project:compileKotlin")
        }
    }

    @DisplayName("project with buildSrc should be compatible with project isolation")
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_7_4
    )
    @JvmGradlePluginTests
    @GradleTest
    fun testProjectIsolationWithBuildSrc(gradleVersion: GradleVersion) {
        project(
            projectName = "kt-63990-buildSrcWithKotlinJvmPlugin",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(configurationCache = null)
        ) {
            build("tasks")
        }
    }

    @DisplayName("Multi-module Android project")
    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_83)
    @AndroidGradlePluginTests
    fun testProjectIsolationAndroid(
       gradleVersion: GradleVersion,
       agpVersion: String,
       jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            projectName = "AndroidIncrementalMultiModule",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug")
        }
    }

    @DisplayName("Kapt multi-module project")
    @OtherGradlePluginTests
    @TestMetadata("kapt2/javacIsLoadedOnce")
    @GradleTest
    fun testProjectIsolationKapt(
        gradleVersion: GradleVersion
    ) {
        project("kapt2/javacIsLoadedOnce", gradleVersion) {
            build("assemble")
        }
    }

    @DisplayName("Kapt with Android multi-module project")
    @AndroidGradlePluginTests
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_83)
    @TestMetadata("kapt2/android-databinding")
    @GradleAndroidTest
    fun testProjectIsolationAndroidWithKapt(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            projectName = "kapt2/android-databinding",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            gradleProperties.appendText(
                """
                |kotlin.jvm.target.validation.mode=warning
                """.trimMargin()
            )

            build("assembleDebug")
        }
    }
}

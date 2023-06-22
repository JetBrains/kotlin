/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency.Type.Regular
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.*
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.jetbrainsAnnotationDependencies
import org.jetbrains.kotlin.gradle.util.kotlinStdlibDependencies
import org.jetbrains.kotlin.gradle.util.resolveIdeDependencies

@GradleTestVersions(minVersion = TestVersions.Gradle.G_8_1)
@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_82)
@AndroidGradlePluginTests
class ExternalAndroidTargetIT : KGPBaseTest() {

    @GradleAndroidTest
    fun `test - simple project - build`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "externalAndroidTarget-simple",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assemble") {
                assertTasksExecuted(":bundleAndroidMainAar")
                assertFileInProjectExists("build/outputs/aar/externalAndroidTarget-simple.aar")
            }
        }
    }

    @GradleAndroidTest
    fun `test - simple project - testOnJvm`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "externalAndroidTarget-simple",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location
        ) {
            build("testAndroidTestOnJvm") {
                assertOutputContains("AndroidTestOnJvm")
                assertOutputContains("useCommonMain: CommonMain")
            }
        }
    }

    @GradleAndroidTest
    fun `test - simple project - ide dependency resolution`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "externalAndroidTarget-simple",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location
        ) {
            resolveIdeDependencies { dependencies ->
                dependencies["androidMain"].assertMatches(
                    kotlinStdlibDependencies,
                    jetbrainsAnnotationDependencies,
                    dependsOnDependency(":/commonMain")
                )

                dependencies["androidTestOnJvm"].assertMatches(
                    kotlinStdlibDependencies,
                    jetbrainsAnnotationDependencies,
                    dependsOnDependency(":/commonTest"),
                    binaryCoordinates("junit:junit:4.13.2"),
                    binaryCoordinates("org.hamcrest:hamcrest-core:1.3"),

                    /*
                    Actually the idiomatic expectation would be two friendSourceDependencies being sent instead of the
                    projectArtifactDependency below.

                    friendSourceDependency(":/commonMain"),
                    friendSourceDependency(":/androidMain"),
                     */
                    projectArtifactDependency(Regular, ":", FilePathRegex(".*/androidMain/full.jar"))
                )
            }
        }
    }
}

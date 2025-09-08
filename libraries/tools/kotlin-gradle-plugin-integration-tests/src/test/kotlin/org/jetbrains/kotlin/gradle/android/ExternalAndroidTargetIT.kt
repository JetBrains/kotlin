/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.dependsOnDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.friendSourceDependency
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.jetbrainsAnnotationDependencies
import org.jetbrains.kotlin.gradle.util.kotlinStdlibDependencies
import org.jetbrains.kotlin.gradle.util.resolveIdeDependencies
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.moveTo
import kotlin.io.path.readText
import kotlin.test.fail

// We are using the latest available AGP in this test suite as a max version
// to ensure AGP and MPP integration is not broken.
// This integration allows AGP to configure android target in MPP.
@AndroidTestVersions(
    minVersion = TestVersions.AGP.AGP_82,
    maxVersion = TestVersions.AGP.AGP_811,
    additionalVersions = [
        TestVersions.AGP.AGP_83,
        TestVersions.AGP.AGP_84,
        TestVersions.AGP.AGP_85,
        TestVersions.AGP.AGP_86,
        TestVersions.AGP.AGP_87,
        TestVersions.AGP.AGP_88,
        TestVersions.AGP.AGP_89,
        TestVersions.AGP.AGP_810,
    ],
)
@AndroidGradlePluginTests
class ExternalAndroidTargetIT : KGPBaseTest() {

    @GradleAndroidTest
    fun `test - simple project - build`(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "externalAndroidTarget-simple",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location
        ) {
            modifyProjectForAGPVersion(androidVersion)

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
            modifyProjectForAGPVersion(androidVersion)

            // Use different task name based on the AGP version
            val agpVersion = TestVersions.AgpCompatibilityMatrix.fromVersion(androidVersion)
            val taskName = when {
                agpVersion >= TestVersions.AgpCompatibilityMatrix.AGP_88 -> "testAndroidHostTest"
                else -> "testAndroidTestOnJvm"
            }

            build(taskName, forwardBuildOutput = true) {
                // Check for different output text based on the AGP version
                when {
                    agpVersion >= TestVersions.AgpCompatibilityMatrix.AGP_88 ->
                        assertOutputContains("AndroidHostTest")
                    else ->
                        assertOutputContains("AndroidTestOnJvm")
                }
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
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion).disableConfigurationCache_KT70416(),
            buildJdk = jdkVersion.location,
        ) {
            modifyProjectForAGPVersion(androidVersion)
            resolveIdeDependencies(
                buildOptions = buildOptions.suppressAgpWarningSinceGradle814(gradleVersion)
            ) { dependencies ->
                dependencies["androidMain"].assertMatches(
                    kotlinStdlibDependencies,
                    jetbrainsAnnotationDependencies,
                    dependsOnDependency(":/commonMain")
                )

                // Use different source set name based on the AGP version
                val agpVersion = TestVersions.AgpCompatibilityMatrix.fromVersion(androidVersion)
                val sourceSetName = when {
                    agpVersion >= TestVersions.AgpCompatibilityMatrix.AGP_88 -> "androidHostTest"
                    else -> "androidTestOnJvm"
                }

                dependencies[sourceSetName].assertMatches(
                    kotlinStdlibDependencies,
                    jetbrainsAnnotationDependencies,
                    dependsOnDependency(":/commonTest"),
                    binaryCoordinates("junit:junit:4.13.2"),
                    binaryCoordinates("org.hamcrest:hamcrest-core:1.3"),
                    friendSourceDependency(":/commonMain"),
                    friendSourceDependency(":/androidMain"),
                )
            }
        }
    }

    @GradleAndroidTest
    fun `test - simple project - pom dependencies rewritten`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk, @TempDir localRepoDir: Path,
    ) {
        val lowestAGPVersion = AndroidGradlePluginVersion(TestVersions.AGP.AGP_810)
        val currentAGPVersion = AndroidGradlePluginVersion(androidVersion)
        val buildOptions = if (currentAGPVersion < lowestAGPVersion) {
            // https://issuetracker.google.com/issues/389951197
            defaultBuildOptions.disableIsolatedProjects()
        } else {
            defaultBuildOptions
        }
        project(
            "externalAndroidTarget-project2project",
            gradleVersion,
            buildOptions = buildOptions
                .copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
            localRepoDir = localRepoDir
        ) {
            modifyProjectForAGPVersion(androidVersion)

            build("publish") {
                val pomFile = localRepoDir.resolve("app/app-android/1.0/app-android-1.0.pom")
                assertFileExists(pomFile)

                fun String.removeWhiteSpaces() = replace("\\s+".toRegex(), "")
                val pomText = pomFile.readText()
                val expectedDependency = """
                    <dependency>
                      <groupId>sample</groupId>
                      <artifactId>tcs-android</artifactId>
                      <version>2.0</version>
                      <scope>compile</scope>
                    </dependency>
                """.trimIndent()

                if (expectedDependency.removeWhiteSpaces() !in pomText.removeWhiteSpaces())
                    fail("Expected to find\n$expectedDependency\nIn POM file\n$pomText")
            }
        }
    }

    private fun TestProject.modifyProjectForAGPVersion(androidVersion: String) {
        val agpVersion = TestVersions.AgpCompatibilityMatrix.fromVersion(androidVersion)
        buildGradleKts.modify {
            val withAndroidTestMethod = when {
                agpVersion >= TestVersions.AgpCompatibilityMatrix.AGP_88 -> "withHostTest {}"
                else -> "withAndroidTestOnJvm {}"
            }
            val androidTestSourceSetName = when {
                agpVersion >= TestVersions.AgpCompatibilityMatrix.AGP_88 -> "androidHostTest"
                else -> "androidTestOnJvm"
            }
            it.replace("<host-test-dsl>", withAndroidTestMethod)
                .replace("<host-test-source-set-name>", androidTestSourceSetName)
        }

        if (agpVersion >= TestVersions.AgpCompatibilityMatrix.AGP_88) {
            projectPath.resolve("src/androidTestOnJvm")
                .moveTo(projectPath.resolve("src/androidHostTest"))
        }
    }
}

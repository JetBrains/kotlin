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
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.jetbrainsAnnotationDependencies
import org.jetbrains.kotlin.gradle.util.kotlinStdlibDependencies
import org.jetbrains.kotlin.gradle.util.resolveIdeDependencies
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.fail

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
        project(
            "externalAndroidTarget-project2project",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
            localRepoDir = localRepoDir
        ) {
            build("publish", buildOptions = buildOptions.copy(configurationCache = true)) {
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
}

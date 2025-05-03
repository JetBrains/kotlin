/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@GradleTestVersions(additionalVersions = [TestVersions.Gradle.G_8_9])
class KotlinTopLevelDependenciesIT : KGPBaseTest() {

    @DisplayName("Test kotlin { dependencies {} } block with kotlinx-coroutines in Kotlin Build Script")
    @GradleTest
    fun testKotlinTopLevelDependenciesWithCoroutinesKotlin(gradleVersion: GradleVersion) {
        project("kotlinTopLevelDependenciesSample", gradleVersion) {
            if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_9)) {
                buildAndFail("assemble") {
                    // TODO: Fixme, improve error message here
                    assertOutputContains("Unresolved reference. None of the following candidates is applicable because of receiver type mismatch:")
                    assertOutputContains("public open fun NamedDomainObjectProvider<KotlinSourceSet>.invoke(configure: KotlinSourceSet.() -> Unit): Unit defined in org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension")
                }
                return@project
            } else {
                testKotlinTopLevelDependenciesWithCoroutines()
            }
        }
    }

    @DisplayName("Test kotlin { dependencies {} } block with kotlinx-coroutines in Groovy Build Script")
    @GradleTest
    fun testKotlinTopLevelDependenciesWithCoroutinesGroovy(gradleVersion: GradleVersion) {
        project("kotlinTopLevelDependenciesSampleGroovy", gradleVersion) {
            if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_9)) {
                buildAndFail("assemble") {
                    assertOutputContains("Kotlin top-level dependencies is not available in Gradle 7.6.3. Min supported version is Gradle 8.9")
                }
                return@project
            } else {
                testKotlinTopLevelDependenciesWithCoroutines()
            }
        }
    }

    private fun TestProject.testKotlinTopLevelDependenciesWithCoroutines() {
        build("assemble")

        // Verify that the project is publishable
        build("publish")

        // Verify that the published POM contains the kotlinx-coroutines dependency
        val pomFile = projectPath.resolve("build/repo/test/kotlinTopLevelDependenciesSample/1.0/kotlinTopLevelDependenciesSample-1.0.pom")
        assertFileExists(pomFile)
        assertFileContains(pomFile, "kotlinx-coroutines-core")
    }
}

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonTest
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTopLevelDependenciesNotAvailable
import org.jetbrains.kotlin.gradle.plugin.mpp.MinSupportedGradleVersionWithDependencyCollectorsString
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests that verify dependencies added to the top-level block are visible in commonMain and commonTest source sets.
 */
class KotlinTopLevelDependenciesTest : SourceSetDependenciesResolution() {

    private fun Project.defaultTargets() {
        kotlin { jvm(); linuxX64(); js(); }
    }

    @Test
    fun topLevelDependenciesVisibleInCommonSourceSets() {
        val project = buildProjectWithMPP {
            defaultTargets()
            kotlin {
                dependencies {
                    api("test:api:1.0")
                    implementation("test:implementation:1.0")
                    compileOnly("test:compileOnly:1.0")
                    runtimeOnly("test:runtimeOnly:1.0")

                    testImplementation("test:test-implementation:1.0")
                    testCompileOnly("test:test-compileOnly:1.0")
                    testRuntimeOnly("test:test-runtimeOnly:1.0")
                }

                sourceSets.commonMain.dependencies {
                    api("test:commonMainApi:1.0")
                    implementation("test:commonMainImplementation:1.0")
                    compileOnly("test:commonMainCompileOnly:1.0")
                    runtimeOnly("test:commonMainRuntimeOnly:1.0")
                }

                sourceSets.commonTest.dependencies {
                    implementation("test:commonTestImplementation:1.0")
                    compileOnly("test:commonTestCompileOnly:1.0")
                    runtimeOnly("test:commonTestRuntimeOnly:1.0")
                }
            }
        }
        project.evaluate()

        fun assertDependencies(configurationName: String, vararg dependencyNotations: String) {
            val configuration = project.configurations.getByName(configurationName)
            assertEquals(
                dependencyNotations.toList(),
                configuration.allDependencies.map { it.toString() },
                "Expected $configurationName to contain exactly ${dependencyNotations.toList()}"
            )
        }

        val commonMain = project.multiplatformExtension.sourceSets.commonMain.get()
        assertDependencies(
            commonMain.apiConfigurationName,
            "test:api:1.0",
            "test:commonMainApi:1.0",
        )
        assertDependencies(
            commonMain.implementationConfigurationName,
            "test:implementation:1.0",
            "test:commonMainImplementation:1.0",
        )
        assertDependencies(
            commonMain.compileOnlyConfigurationName,
            "test:compileOnly:1.0",
            "test:commonMainCompileOnly:1.0",
        )
        assertDependencies(
            commonMain.runtimeOnlyConfigurationName,
            "test:runtimeOnly:1.0",
            "test:commonMainRuntimeOnly:1.0",
        )

        val commonTest = project.multiplatformExtension.sourceSets.commonTest.get()
        assertDependencies(
            commonTest.implementationConfigurationName,
            "test:test-implementation:1.0",
            "test:commonTestImplementation:1.0"
        )
        assertDependencies(
            commonTest.compileOnlyConfigurationName,
            "test:test-compileOnly:1.0",
            "test:commonTestCompileOnly:1.0"
        )
        assertDependencies(
            commonTest.runtimeOnlyConfigurationName,
            "test:test-runtimeOnly:1.0",
            "test:commonTestRuntimeOnly:1.0"
        )
    }

    @Test
    fun topLevelDependenciesAreCorrectlyPropagatedToSourceSets() {
        assertSourceSetDependenciesResolution("topLevelDependenciesAreCorrectlyPropagatedToSourceSets.txt") { project ->
            project.defaultTargets()
            project.kotlin {
                dependencies {
                    api(mockedDependency("top-level-api", "1.0"))
                    implementation(mockedDependency("top-level-implementation", "1.0"))
                    compileOnly(mockedDependency("top-level-compileOnly", "1.0"))
                    runtimeOnly(mockedDependency("top-level-runtimeOnly", "1.0"))

                    testImplementation(mockedDependency("top-level-test-implementation", "1.0"))
                    testCompileOnly(mockedDependency("top-level-test-compileOnly", "1.0"))
                    testRuntimeOnly(mockedDependency("top-level-test-runtimeOnly", "1.0"))
                }
            }
        }
    }

    @Test
    fun testKotlinTopLevelDependenciesNotAvailablePrinting() {
        assertEquals(
            "Kotlin top-level dependencies is not available in Gradle 7.6.3. Minimum supported version is Gradle ${MinSupportedGradleVersionWithDependencyCollectorsString}. Please upgrade your Gradle version or keep using source-set level dependencies block: https://kotl.in/kmp-top-level-dependencies",
            KotlinTopLevelDependenciesNotAvailable(
                GradleVersion.version("7.6.3"),
            ).message
        )
    }
}

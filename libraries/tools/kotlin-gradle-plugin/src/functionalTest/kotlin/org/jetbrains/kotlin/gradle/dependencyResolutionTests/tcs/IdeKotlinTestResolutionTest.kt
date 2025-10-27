/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.anyDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.anyDependsOnDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.anySourceFriendDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.notExpected
import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.kotlinNativeDistributionDependencies
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.util.buildKMPWithAllBackends
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.test.*

@Suppress("FunctionName")
class IdeKotlinTestResolutionTest {
    private fun expectedKotlinTestDependenciesInTestSourceSets(kgpVersion: String) = mapOf(
        "commonTest" to listOf(
            binaryCoordinates("org.jetbrains.kotlin:kotlin-test:annotationsCommonMain:$kgpVersion"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-test:assertionsCommonMain:$kgpVersion"),
            anyDependency()
        ),
        "jvmTest" to listOf(
            binaryCoordinates("org.jetbrains.kotlin:kotlin-test:$kgpVersion"),
            /** This dependency is added depending on Test task framework
             * See: [org.jetbrains.kotlin.gradle.internal.kotlinTestCapabilityForJvmSourceSet] */
            binaryCoordinates("org.jetbrains.kotlin:kotlin-test-junit:$kgpVersion"),
            anyDependency()
        ),
        "linuxX64Test" to listOf(
            // Kotlin/Native have kotlin-test dependencies bundled to native-distribution
            // so there is artifact on "kotlin-test" dependency expected
            anyDependsOnDependency(),
            anySourceFriendDependency(),
            kotlinNativeDistributionDependencies,
        ),
        "jsTest" to listOf(
            binaryCoordinates("org.jetbrains.kotlin:kotlin-test-js:$kgpVersion"),
            anyDependency()
        ),
        "wasmJsTest" to listOf(
            binaryCoordinates("org.jetbrains.kotlin:kotlin-test-wasm-js:$kgpVersion"),
            anyDependency()
        ),
        "wasmWasiTest" to listOf(
            binaryCoordinates("org.jetbrains.kotlin:kotlin-test-wasm-wasi:$kgpVersion"),
            anyDependency()
        )
    )

    private fun verifyKotlinTestDependencies(
        addKotlinTestToProject: Project.() -> Unit
    ) {
        val project = buildKMPWithAllBackends {
            configureRepositoriesForTests()
            addKotlinTestToProject()
        }
        project.evaluate()

        val kgpVersion = project.getKotlinPluginVersion()

        expectedKotlinTestDependenciesInTestSourceSets(kgpVersion).forEach { (sourceSetName, assertions) ->
            project.resolveDependencies(sourceSetName).assertMatches(*assertions.toTypedArray())
        }
    }

    @Test
    fun `kotlin-test from commonTest resolves correctly in KMP projects`() = verifyKotlinTestDependencies {
        kotlin {
            sourceSets.commonTest.dependencies {
                implementation(kotlin("test"))
            }
        }
    }

    @Test
    fun `KT-81849 legacy kotlin-test-common is replaced with kotlin-test`() = verifyKotlinTestDependencies {
        kotlin {
            sourceSets.commonTest.dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
            }
        }
    }

    @Test
    fun `kotlin-test is not added by default`() {
        val project = buildKMPWithAllBackends()
        project.evaluate()

        project.kotlinExtension.sourceSets.forEach {
            val kotlinTestDependency = project.kotlinIdeMultiplatformImport.resolveDependencies(it)
                .firstOrNull { it.coordinates.toString().contains("kotlin-test") }
            if (kotlinTestDependency != null) fail("$kotlinTestDependency should not be resolved in ${it.name}")
        }
    }

    @Test
    fun `kotlin-test in commonMain source set`() {
        val project = buildKMPWithAllBackends {
            configureRepositoriesForTests()

            kotlin {
                sourceSets.commonMain.dependencies {
                    implementation(kotlin("test"))
                }
            }
        }
        project.evaluate()

        val kgpVersion = project.getKotlinPluginVersion()

        val expectedKotlinTestDependenciesInMainSourceSets = mapOf(
            "commonMain" to listOf(
                binaryCoordinates("org.jetbrains.kotlin:kotlin-test:annotationsCommonMain:$kgpVersion"),
                binaryCoordinates("org.jetbrains.kotlin:kotlin-test:assertionsCommonMain:$kgpVersion"),
                anyDependency()
            ),
            "jvmMain" to listOf(
                binaryCoordinates("org.jetbrains.kotlin:kotlin-test:$kgpVersion"),
                // Unlike in jvmTest, in jvmMain it should not be expected,
                // because there is no test-task associated with "jvmMain".
                binaryCoordinates("org.jetbrains.kotlin:kotlin-test-junit:$kgpVersion").notExpected,
                anyDependency()
            ),
            "linuxX64Main" to listOf(
                // Kotlin/Native have kotlin-test dependencies bundled to native-distribution
                // so there is artifact on "kotlin-test" dependency expected
                binaryCoordinates(".*kotlin-test.*".toRegex()).notExpected,
                anyDependsOnDependency(),
                kotlinNativeDistributionDependencies,
            ),
            "jsMain" to listOf(
                binaryCoordinates("org.jetbrains.kotlin:kotlin-test-js:$kgpVersion"),
                anyDependency()
            ),
            "wasmJsMain" to listOf(
                binaryCoordinates("org.jetbrains.kotlin:kotlin-test-wasm-js:$kgpVersion"),
                anyDependency()
            ),
            "wasmWasiMain" to listOf(
                binaryCoordinates("org.jetbrains.kotlin:kotlin-test-wasm-wasi:$kgpVersion"),
                anyDependency()
            )
        )

        expectedKotlinTestDependenciesInMainSourceSets.forEach { (sourceSetName, assertions) ->
            project.resolveDependencies(sourceSetName).assertMatches(*assertions.toTypedArray())
        }

        // test source sets should receive kotlin-test transitively through associated compilations mecanism
        expectedKotlinTestDependenciesInTestSourceSets(kgpVersion).forEach { (sourceSetName, assertions) ->
            project.resolveDependencies(sourceSetName).assertMatches(*assertions.toTypedArray())
        }
    }
}
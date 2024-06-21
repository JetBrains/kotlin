/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.sources.METADATA_CONFIGURATION_NAME_SUFFIX
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.testResolveAllConfigurations
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.utils.addToStdlib.countOccurrencesOf
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@MppGradlePluginTests
class MppMetadataResolutionIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copy(
        nativeOptions = super.defaultBuildOptions.nativeOptions.copy(
            // Use kotlin-native bundle version provided by default in KGP, because it will be pushed in one of the known IT repos for sure
            version = null
        )
    )

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app")
    fun testResolveMppLibDependencyToMetadata(gradleVersion: GradleVersion) {
        val localRepoDir = defaultLocalRepo(gradleVersion)

        project(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
        ) {

            // TODO KT-65528 move publishing config into the actual build.gradle
            buildGradle.append(
                """
                |publishing {
                |  repositories {
                |    maven { url = "${localRepoDir.invariantSeparatorsPathString}" }
                |  }
                |}
                """.trimMargin()
            )

            build("publish")
        }

        project(
            projectName = "new-mpp-lib-and-app/sample-app",
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
        ) {
            buildGradle.append(
                """
                |kotlin.sourceSets {
                |    commonMain {
                |        dependencies {
                |            // add these dependencies to check that they are resolved to metadata
                |            api("com.example:sample-lib:1.0")
                |            compileOnly("com.example:sample-lib:1.0")
                |        }
                |    }
                |}
                """.trimMargin()
            )

            testResolveAllConfigurations { unresolvedConfigurations, buildResult ->
                assertTrue(
                    unresolvedConfigurations.isEmpty(),
                    "Expected no unresolved configurations, but found ${unresolvedConfigurations.size}: $unresolvedConfigurations",
                )

                buildResult.assertOutputContains(">> :commonMainResolvable$METADATA_CONFIGURATION_NAME_SUFFIX --> sample-lib-metadata-1.0.jar")
            }
        }
    }

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app")
    fun testResolveMppProjectDependencyToMetadata(gradleVersion: GradleVersion) {
        project(
            projectName = "new-mpp-lib-and-app/sample-app",
            gradleVersion = gradleVersion,
        ) {

            includeOtherProjectAsSubmodule(
                otherProjectName = "sample-lib",
                pathPrefix = "new-mpp-lib-and-app",
            )

            buildGradle.replaceText(
                """'com.example:sample-lib:1.0'""",
                """project(":sample-lib")""",
            )

            testResolveAllConfigurations { unresolvedConfigurations, buildResult ->
                assertTrue(
                    unresolvedConfigurations.isEmpty(),
                    "Expected no unresolved configurations, but found ${unresolvedConfigurations.size}: $unresolvedConfigurations",
                )

                buildResult.assertOutputContains(">> :commonMainResolvable$METADATA_CONFIGURATION_NAME_SUFFIX --> sample-lib-metadata-1.0.jar")
            }
        }
    }

    @GradleTest
    @TestMetadata(value = "kt-69310-duplicateMetadataLibrariesInClasspath")
    fun testNoDuplicateLibrariesInDiamondStructures(gradleVersion: GradleVersion) {
        project(
            projectName = "kt-69310-duplicateMetadataLibrariesInClasspath",
            gradleVersion = gradleVersion
        ) {
            build(":compileLinuxMainKotlinMetadata") {
                assertOutputDoesNotContain("""KLIB resolver.*The same 'unique_name=.*' found in more than one library""".toRegex())
                val arguments = extractNativeCompilerTaskArguments(":compileLinuxMainKotlinMetadata")
                assertEquals(
                    1,
                    arguments.countOccurrencesOf("kotlinx-kotlinx-coroutines-core-1.8.1-commonMain"),
                    "Unexpected number of kotlinx-kotlinx-coroutines-core-1.8.1-commonMain"
                )
                assertEquals(
                    1,
                    arguments.countOccurrencesOf("kotlinx-kotlinx-coroutines-core-1.8.1-concurrentMain"),
                    "Unexpected number of kotlinx-kotlinx-coroutines-core-1.8.1-concurrentMain"
                )
                assertEquals(
                    1,
                    arguments.countOccurrencesOf("kotlinx-kotlinx-coroutines-core-1.8.1-nativeMain"),
                    "Unexpected number of kotlinx-kotlinx-coroutines-core-1.8.1-concurrentMain"
                )
            }
        }
    }
}


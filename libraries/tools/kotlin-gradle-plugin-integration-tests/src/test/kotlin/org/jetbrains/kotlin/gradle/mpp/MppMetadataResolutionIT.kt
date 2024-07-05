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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@MppGradlePluginTests
class MppMetadataResolutionIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app")
    fun testResolveMppLibDependencyToMetadata(gradleVersion: GradleVersion) {
        project(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
            localRepoDir = defaultLocalRepo(gradleVersion),
        ) {
            build("publish")
        }

        project(
            projectName = "new-mpp-lib-and-app/sample-app",
            gradleVersion = gradleVersion,
            localRepoDir = defaultLocalRepo(gradleVersion),
        ) {
            buildGradle.replaceText(
                "shouldBeJs = true",
                "shouldBeJs = false",
            )
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
                val filteredUnresolvedConfigurations = unresolvedConfigurations
                    // TODO: KT-69695 Exclude kotlinNativeBundleConfiguration as it can't be resolved on CI
                    .minus(":kotlinNativeBundleConfiguration")

                assertTrue(
                    filteredUnresolvedConfigurations.isEmpty(),
                    "Expected no unresolved configurations, but found ${filteredUnresolvedConfigurations.size}: $filteredUnresolvedConfigurations",
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
                """"com.example:sample-lib:1.0"""",
                """project(":sample-lib")""",
            )

            testResolveAllConfigurations { unresolvedConfigurations, buildResult ->
                val filteredUnresolvedConfigurations = unresolvedConfigurations
                    // TODO: KT-69695 Exclude kotlinNativeBundleConfiguration as it can't be resolved on CI
                    .minus(":kotlinNativeBundleConfiguration")

                assertTrue(
                    filteredUnresolvedConfigurations.isEmpty(),
                    "Expected no unresolved configurations, but found ${filteredUnresolvedConfigurations.size}: $filteredUnresolvedConfigurations",
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

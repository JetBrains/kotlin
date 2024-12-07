/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.testResolveAllConfigurations
import org.jetbrains.kotlin.test.TestMetadata
import kotlin.test.assertTrue

@MppGradlePluginTests
class MppMetadataCompileClasspathIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copy(
        nativeOptions = super.defaultBuildOptions.nativeOptions.copy(
            // Use kotlin-native bundle version provided by default in KGP, because it will be pushed in one of the known IT repos for sure
            version = null
        )
    )

    @GradleTest
    @TestMetadata(value = "kt-50925-resolve-metadata-compile-classpath")
    fun testResolveMetadataCompileClasspathKt50925(gradleVersion: GradleVersion) {
        val localRepoDir = defaultLocalRepo(gradleVersion)

        project(
            projectName = "kt-50925-resolve-metadata-compile-classpath/lib",
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
        ) {
            build("publish")
        }

        project(
            projectName = "kt-50925-resolve-metadata-compile-classpath/app",
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
        ) {
            testResolveAllConfigurations { unresolvedConfigurations, result ->
                assertTrue(unresolvedConfigurations.isEmpty(), "Unresolved configurations: $unresolvedConfigurations")

                with(result) {
                    assertOutputContains(">> :metadataCompileClasspath --> lib-metadata-1.0.jar")
                    assertOutputContains(">> :metadataCompileClasspath --> subproject-metadata.jar")
                    assertOutputContains(">> :metadataCommonMainCompileClasspath --> lib-metadata-1.0.jar")
                    assertOutputContains(">> :metadataCommonMainCompileClasspath --> subproject-metadata.jar")
                }
            }
        }
    }
}

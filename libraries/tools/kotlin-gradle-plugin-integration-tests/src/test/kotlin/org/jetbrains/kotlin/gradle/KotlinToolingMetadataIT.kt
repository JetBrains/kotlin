/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.tooling.BuildKotlinToolingMetadataTask
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata
import org.jetbrains.kotlin.tooling.parseJsonOrThrow
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.readText
import kotlin.test.assertEquals

@MppGradlePluginTests
@DisplayName("Multiplatform metadata for tooling")
class KotlinToolingMetadataMppIT : KGPBaseTest() {
    private val TestProject.defaultKotlinToolingMetadataJsonPath
        get() = projectPath.resolve("build/kotlinToolingMetadata/kotlin-tooling-metadata.json")


    private val buildKotlinToolingMetadataTaskName
        get() = BuildKotlinToolingMetadataTask.defaultTaskName

    @GradleTest
    @DisplayName("Check published metadata contains right data")
    fun checkPublishedMetadata(
        gradleVersion: GradleVersion,
        @TempDir localRepository: Path,
    ) {
        project(
            projectName = "new-mpp-published",
            gradleVersion = gradleVersion,
            localRepoDir = localRepository
        ) {

            build("publish") {
                assertTasksExecuted(":$buildKotlinToolingMetadataTaskName")
                /* Check metadata file in build dir */
                assertFileExists(defaultKotlinToolingMetadataJsonPath)
                val metadataJson = defaultKotlinToolingMetadataJsonPath.readText()
                val metadata = KotlinToolingMetadata.parseJsonOrThrow(metadataJson)
                assertEquals(
                    listOfNotNull(
                        KotlinPlatformType.common.name,
                        KotlinPlatformType.jvm.name,
                        KotlinPlatformType.js.name,
                        KotlinPlatformType.native.name,
                        KotlinPlatformType.wasm.name,
                    ).sorted(),
                    metadata.projectTargets.map { it.platformType }.sorted()
                )

                /* Check metadata file in published repository */
                val publishedMetadataJson = localRepository.resolve(
                    "com/example/bar/my-lib-bar/1.0/my-lib-bar-1.0-kotlin-tooling-metadata.json"
                ).readText()

                assertEquals(
                    metadataJson, publishedMetadataJson,
                    "Expected published kotlin-tooling-metadata.json to contain same content as in buildDir"
                )
            }

            /* Checking UP-TO-DATE behaviour */
            build("publish") {
                // Nothing changed. Should be up to date
                assertTasksUpToDate(":$buildKotlinToolingMetadataTaskName")
            }

            // Adding macos target
            buildGradleKts.replaceText("linuxX64()", "linuxX64()\nmacosX64()")

            build("publish") {
                assertTasksExecuted(":$buildKotlinToolingMetadataTaskName")
                val metadata = KotlinToolingMetadata.parseJsonOrThrow(defaultKotlinToolingMetadataJsonPath.readText())
                assertEquals(
                    listOf(KonanTarget.LINUX_X64.name, KonanTarget.MACOS_X64.name).sorted(),
                    metadata.projectTargets.mapNotNull { it.extras.native?.konanTarget }.sorted()
                )
            }

        }
    }

    @GradleTest
    @DisplayName("KotlinToolingMetadata should be not published when disabled")
    fun checkPublishingWithKotlinToolingMetadataArtifactDisabled(
        gradleVersion: GradleVersion,
        @TempDir localRepository: Path,
    ) {
        project(
            projectName = "new-mpp-published",
            gradleVersion = gradleVersion,
            localRepoDir = localRepository
        ) {
            gradleProperties.appendText("\nkotlin.mpp.enableKotlinToolingMetadataArtifact=false")
            build("publish") {
                assertFileNotExists(defaultKotlinToolingMetadataJsonPath)
                assertTasksAreNotInTaskGraph(":$buildKotlinToolingMetadataTaskName")
            }
        }
    }

    @GradleTest
    @DisplayName("KotlinToolingMetadata tasks are avaialbe in Kotlin JS browser project")
    fun tasksAreAvailableInKotlinJsBrowser(
        gradleVersion: GradleVersion,
        @TempDir localRepository: Path
    ) {
        project(
            projectName = "kotlin-js-browser-project",
            gradleVersion = gradleVersion,
            localRepoDir = localRepository
        ) {
            build(buildKotlinToolingMetadataTaskName) {
                assertTasksExecuted(":app:$buildKotlinToolingMetadataTaskName")
                assertTasksExecuted(":base:$buildKotlinToolingMetadataTaskName")
                assertTasksExecuted(":lib:$buildKotlinToolingMetadataTaskName")
            }
        }
    }
}

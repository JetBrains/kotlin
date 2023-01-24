/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.KOTLIN_VERSION
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency.Type.Regular
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.*
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@MppGradlePluginTests
@DisplayName("Tests for multiplatform with composite builds")
class MppCompositeBuildIT : KGPBaseTest() {
    @GradleTest
    fun `test - sample0 - ide dependencies`(gradleVersion: GradleVersion) {
        val producer = project("mpp-composite-build/sample0/producerBuild", gradleVersion)

        project("mpp-composite-build/sample0/consumerBuild", gradleVersion) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.toUri().path)
            resolveIdeDependencies(":consumerA") { dependencies ->
                dependencies["commonMain"].assertMatches(
                    regularSourceDependency("producerBuild::producerA/commonMain"),
                    kotlinStdlibDependencies
                )

                dependencies["nativeMain"].assertMatches(
                    dependsOnDependency(":consumerA/commonMain"),
                    regularSourceDependency("producerBuild::producerA/commonMain"),
                    regularSourceDependency("producerBuild::producerA/nativeMain"),
                    regularSourceDependency("producerBuild::producerA/linuxMain"),
                    kotilnNativeDistributionDependencies
                )

                dependencies["linuxMain"].assertMatches(
                    dependencies["nativeMain"],
                    dependsOnDependency(":consumerA/nativeMain"),
                )

                dependencies["linuxX64Main"].assertMatches(
                    dependsOnDependency(":consumerA/commonMain"),
                    dependsOnDependency(":consumerA/nativeMain"),
                    dependsOnDependency(":consumerA/linuxMain"),
                    projectArtifactDependency(Regular, "producerBuild::producerA", FilePathRegex(".*/linuxX64/main/klib/producerA.klib")),
                    kotilnNativeDistributionDependencies
                )
            }
        }
    }

    @GradleTest
    fun `test - sample0 - assemble`(gradleVersion: GradleVersion) {
        val producer = project("mpp-composite-build/sample0/producerBuild", gradleVersion)

        project("mpp-composite-build/sample0/consumerBuild", gradleVersion) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.toUri().path)
            build("cleanNativeDistributionCommonization")

            build("assemble") {
                assertTasksExecuted(":consumerA:compileCommonMainKotlinMetadata")
                assertTasksExecuted(":consumerA:compileNativeMainKotlinMetadata")
                assertTasksExecuted(":consumerA:compileNativeMainKotlinMetadata")
                assertTasksExecuted(":consumerA:compileKotlinLinuxX64")
                assertTasksExecuted(":consumerA:compileKotlinJvm")
            }

            build("assemble") {
                assertTasksUpToDate(":consumerA:compileCommonMainKotlinMetadata")
                assertTasksUpToDate(":consumerA:compileNativeMainKotlinMetadata")
                assertTasksUpToDate(":consumerA:compileNativeMainKotlinMetadata")
                assertTasksUpToDate(":consumerA:compileKotlinLinuxX64")
                assertTasksUpToDate(":consumerA:compileKotlinJvm")
            }
        }
    }

    @GradleTest
    fun `test - sample0 - assemble - enableCInteropCommonization=true`(gradleVersion: GradleVersion) {
        val producer = project("mpp-composite-build/sample0/producerBuild", gradleVersion)

        project(
            "mpp-composite-build/sample0/consumerBuild", gradleVersion, defaultBuildOptions.copy(
                freeArgs = defaultBuildOptions.freeArgs + "-Pkotlin.mpp.enableCInteropCommonization=true"
            )
        ) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.toUri().path)
            build("assemble") {
                assertTasksExecuted(":consumerA:compileCommonMainKotlinMetadata")
                assertTasksExecuted(":consumerA:compileNativeMainKotlinMetadata")
                assertTasksExecuted(":consumerA:compileNativeMainKotlinMetadata")
                assertTasksExecuted(":consumerA:compileKotlinLinuxX64")
                assertTasksExecuted(":consumerA:compileKotlinJvm")
            }

            build("assemble") {
                assertTasksUpToDate(":consumerA:compileCommonMainKotlinMetadata")
                assertTasksUpToDate(":consumerA:compileNativeMainKotlinMetadata")
                assertTasksUpToDate(":consumerA:compileNativeMainKotlinMetadata")
                assertTasksUpToDate(":consumerA:compileKotlinLinuxX64")
                assertTasksUpToDate(":consumerA:compileKotlinJvm")
            }
        }
    }

    @GradleTest
    fun `test - sample1 - ide dependencies`(gradleVersion: GradleVersion) {
        project("mpp-composite-build/sample1", gradleVersion) {
            projectPath.resolve("included-build").addDefaultBuildFiles()
            buildGradleKts.replaceText("<kgp_version>", KOTLIN_VERSION)
            projectPath.resolve("included-build/build.gradle.kts").replaceText("<kgp_version>", KOTLIN_VERSION)

            resolveIdeDependencies { dependencies ->
                dependencies["commonMain"].assertMatches(
                    kotlinStdlibDependencies,
                    regularSourceDependency("included-build::included/commonMain")
                )

                dependencies["jvmMain"].assertMatches(
                    kotlinStdlibDependencies,
                    jetbrainsAnnotationDependencies,

                    dependsOnDependency(":/commonMain"),
                    projectArtifactDependency(
                        Regular, "included-build::included",
                        FilePathRegex(".*/included-build/included/build/libs/included-jvm.jar")
                    )
                )
            }
        }
    }

    @GradleTest
    fun `test - sample1 - assemble and execute`(gradleVersion: GradleVersion) {
        project("mpp-composite-build/sample1", gradleVersion) {
            projectPath.resolve("included-build").addDefaultBuildFiles()
            buildGradleKts.replaceText("<kgp_version>", KOTLIN_VERSION)
            projectPath.resolve("included-build/build.gradle.kts").replaceText("<kgp_version>", KOTLIN_VERSION)

            build("assemble") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
                assertTasksExecuted(":compileKotlinJvm")
                assertTasksExecuted(":compileKotlinJs")
            }

            build("assemble") {
                assertTasksUpToDate(":compileCommonMainKotlinMetadata")
                assertTasksUpToDate(":compileKotlinJvm")
                assertTasksUpToDate(":compileKotlinJs")
            }

            build("check") {
                assertTasksExecuted(":jvmTest")
                assertTasksExecuted(":jsTest")
            }
        }
    }

    @GradleTest
    fun `test - sample1 - assemble and execute - included build using older version of Kotlin`(gradleVersion: GradleVersion) {
        project("mpp-composite-build/sample1", gradleVersion) {
            projectPath.resolve("included-build").addDefaultBuildFiles()
            buildGradleKts.replaceText("<kgp_version>", KOTLIN_VERSION)
            projectPath.resolve("included-build/build.gradle.kts").replaceText("<kgp_version>", "1.7.21")

            build("assemble") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
                assertTasksExecuted(":compileKotlinJvm")
                assertTasksExecuted(":compileKotlinJs")
            }

            build("check") {
                assertTasksExecuted(":jvmTest")
                assertTasksExecuted(":jsTest")
            }
        }
    }

    @OsCondition(enabledOnCI = [OS.MAC], supportedOn = [OS.MAC])
    @GradleTest
    fun `test - sample2-withHostSpecificTargets - assemble`(gradleVersion: GradleVersion) {
        val producer = project("mpp-composite-build/sample2-withHostSpecificTargets/producerBuild", gradleVersion)

        project("mpp-composite-build/sample2-withHostSpecificTargets/consumerBuild", gradleVersion) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.toUri().path)
            build("cleanNativeDistributionCommonization")

            build("assemble") {
                assertTasksExecuted(":consumerA:compileCommonMainKotlinMetadata")
                assertTasksExecuted(":consumerA:compileNativeMainKotlinMetadata")
                assertTasksExecuted(":consumerA:compileNativeMainKotlinMetadata")
                assertTasksExecuted(":consumerA:compileKotlinIosX64")
                assertTasksExecuted(":consumerA:compileKotlinJvm")
            }

            build("assemble") {
                assertTasksUpToDate(":consumerA:compileCommonMainKotlinMetadata")
                assertTasksUpToDate(":consumerA:compileNativeMainKotlinMetadata")
                assertTasksUpToDate(":consumerA:compileNativeMainKotlinMetadata")
                assertTasksUpToDate(":consumerA:compileKotlinIosX64")
                assertTasksUpToDate(":consumerA:compileKotlinJvm")
            }
        }
    }
}

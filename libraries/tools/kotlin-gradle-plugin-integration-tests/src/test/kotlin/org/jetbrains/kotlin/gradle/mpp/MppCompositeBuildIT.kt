/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.configuration.WarningMode
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
                    kotlinNativeDistributionDependencies,
                    binaryCoordinates(Regex(".*stdlib-common:.*")) /* KT-56278 */
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
                    kotlinNativeDistributionDependencies,
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

    @GradleTest
    fun `test - sample3-KT-56198-singleTargetMpp-includingJvm - ide dependencies`(gradleVersion: GradleVersion) {
        val producer = project("mpp-composite-build/sample3-KT-56198-singleTargetMpp-includingJvm/producerBuild", gradleVersion)

        project("mpp-composite-build/sample3-KT-56198-singleTargetMpp-includingJvm/consumerBuild", gradleVersion) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.toUri().path)
            resolveIdeDependencies(":consumerA") { dependencies ->
                assertOutputDoesNotContain("e: org.jetbrains.kotlin.gradle.plugin.ide")
                dependencies["commonMain"].assertMatches(
                    kotlinStdlibDependencies,
                    jetbrainsAnnotationDependencies,
                    projectArtifactDependency(
                        Regular, "producerBuild::producerA", FilePathRegex(".*producerA/build/libs/producerA-1.0.0-SNAPSHOT.jar")
                    )
                )

                dependencies["jvmMain"].assertMatches(
                    kotlinStdlibDependencies,
                    jetbrainsAnnotationDependencies,
                    dependsOnDependency(":consumerA/commonMain"),
                    projectArtifactDependency(
                        Regular, "producerBuild::producerA", FilePathRegex(".*producerA/build/libs/producerA-1.0.0-SNAPSHOT.jar")
                    )
                )
            }
        }
    }

    @GradleTest
    fun `test - sample4-KT-37051-withCInterop`(gradleVersion: GradleVersion) {
        val producer = project("mpp-composite-build/sample4-KT-37051-withCInterop/producerBuild", gradleVersion)

        project(
            "mpp-composite-build/sample4-KT-37051-withCInterop/consumerBuild", gradleVersion,
            buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Fail)
        ) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.toUri().path)

            build(":consumerA:assemble") {
                assertTasksExecuted(":consumerA:compileKotlinLinuxX64")
                assertOutputDoesNotContain("w: duplicate library")
            }
        }
    }

    @GradleTest
    fun `test - sample5-KT-56536-rootProject_name - assemble`(gradleVersion: GradleVersion) {
        val producer = project("mpp-composite-build/sample5-KT-56536-rootProject.name/producerBuild", gradleVersion)

        project("mpp-composite-build/sample5-KT-56536-rootProject.name/consumerBuild", gradleVersion) {
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
    fun `test - sample5-KT-56536-rootProject_name - ide dependencies`(gradleVersion: GradleVersion) {
        val producer = project("mpp-composite-build/sample5-KT-56536-rootProject.name/producerBuild", gradleVersion)

        project("mpp-composite-build/sample5-KT-56536-rootProject.name/consumerBuild", gradleVersion) {
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
                    kotlinNativeDistributionDependencies,
                    binaryCoordinates(Regex(".*stdlib-common:.*")) /* KT-56278 */
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
                    kotlinNativeDistributionDependencies,
                )
            }
        }
    }

    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_1)
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_70)
    @GradleAndroidTest
    fun `test - sample6-KT-56712-umbrella-composite`(
        gradleVersion: GradleVersion, agpVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        val producer = project("mpp-composite-build/sample6-KT-56712-umbrella-composite/producer", gradleVersion)
        val consumerA = project("mpp-composite-build/sample6-KT-56712-umbrella-composite/consumerA", gradleVersion)
        val consumerB = project("mpp-composite-build/sample6-KT-56712-umbrella-composite/consumerB", gradleVersion)

        project(
            "mpp-composite-build/sample6-KT-56712-umbrella-composite/composite", gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion), buildJdk = jdkVersion.location
        ) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.toUri().path)
            settingsGradleKts.toFile().replaceText("<consumerA_path>", consumerA.projectPath.toUri().path)
            settingsGradleKts.toFile().replaceText("<consumerB_path>", consumerB.projectPath.toUri().path)

            build(":consumerA:compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":consumerA:compileCommonMainKotlinMetadata")
            }

            build(":consumerB:compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":consumerB:compileCommonMainKotlinMetadata")
            }

            build(":consumerA:resolveIdeDependencies") {
                consumerA.readIdeDependencies()["commonMain"].assertMatches(
                    regularSourceDependency("producer::/commonMain"),
                    kotlinStdlibDependencies
                )
            }

            build(":consumerB:resolveIdeDependencies") {
                consumerB.readIdeDependencies()["commonMain"].assertMatches(
                    regularSourceDependency("producer::/commonMain"),
                    kotlinStdlibDependencies
                )
            }

            build(":producer:resolveIdeDependencies") {
                producer.readIdeDependencies()["commonMain"].assertMatches(
                    kotlinStdlibDependencies
                )
            }
        }
    }
}

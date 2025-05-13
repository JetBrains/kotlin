/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.internal.os.OperatingSystem
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.KOTLIN_VERSION
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency.Type.Regular
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.*
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.utils.addToStdlib.countOccurrencesOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.appendText
import kotlin.test.assertEquals
import kotlin.test.assertIs

@MppGradlePluginTests
@DisplayName("Tests for multiplatform with composite builds")
class MppCompositeBuildIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.disableConfigurationCache_KT70416()

    @GradleTest
    fun `test - sample0 - ide dependencies`(gradleVersion: GradleVersion) {
        val producer = project("mpp-composite-build/sample0/producerBuild", gradleVersion)

        project("mpp-composite-build/sample0/consumerBuild", gradleVersion) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.toUri().path)
            resolveIdeDependencies(":consumerA") { dependencies ->
                dependencies["commonMain"].assertMatches(
                    regularSourceDependency(":producerBuild::producerA/commonMain"),
                    kotlinStdlibDependencies
                )

                dependencies["nativeMain"].assertMatches(
                    dependsOnDependency(":consumerA/commonMain"),
                    regularSourceDependency(":producerBuild::producerA/commonMain"),
                    regularSourceDependency(":producerBuild::producerA/nativeMain"),
                    regularSourceDependency(":producerBuild::producerA/linuxMain"),
                    kotlinNativeDistributionDependencies,
                )

                dependencies["linuxMain"].assertMatches(
                    dependencies["nativeMain"],
                    dependsOnDependency(":consumerA/nativeMain"),
                )

                dependencies["linuxX64Main"].assertMatches(
                    dependsOnDependency(":consumerA/commonMain"),
                    dependsOnDependency(":consumerA/nativeMain"),
                    dependsOnDependency(":consumerA/linuxMain"),
                    projectArtifactDependency(Regular, ":producerBuild::producerA", FilePathRegex(".*/linuxX64/main/klib/producerA")),
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

    /**
     * Test that verifies that after moving to 'buildPath' and 'buildName' in project coordinates (1.9.20),
     * the shape of the resolved coordinate are the same across different versions of Gradle.
     */
    @GradleTest
    fun `test - sample0 - buildId buildPath buildName`(gradleVersion: GradleVersion) {
        val producer = project("mpp-composite-build/sample0/producerBuild", gradleVersion)

        project("mpp-composite-build/sample0/consumerBuild", gradleVersion) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.toUri().path)
            resolveIdeDependencies(":consumerA") { dependencies ->
                /* Pick some known dependency  and run check on it */
                val dependency = dependencies["commonMain"].getOrFail(regularSourceDependency(":producerBuild::producerA/commonMain"))
                assertIs<IdeaKotlinSourceDependency>(dependency)
                val projectCoordinates = dependency.coordinates.project
                @Suppress("DEPRECATION")
                assertEquals("producerBuild", projectCoordinates.buildId)
                assertEquals("producerBuild", projectCoordinates.buildName)
                assertEquals(":producerBuild", projectCoordinates.buildPath)
                assertEquals(":producerA", projectCoordinates.projectPath)
                assertEquals("producerA", projectCoordinates.projectName)
            }
        }
    }

    @GradleTest
    fun `test - sample1 - ide dependencies`(gradleVersion: GradleVersion) {
        project("mpp-composite-build/sample1", gradleVersion) {
            projectPath.resolve("included-build").addDefaultSettingsToSettingsGradle(gradleVersion)
            buildGradleKts.replaceText("<kgp_version>", KOTLIN_VERSION)
            projectPath.resolve("included-build/build.gradle.kts").replaceText("<kgp_version>", KOTLIN_VERSION)

            resolveIdeDependencies { dependencies ->
                dependencies["commonMain"].assertMatches(
                    kotlinStdlibDependencies,
                    regularSourceDependency(":included-build::included/commonMain")
                )

                dependencies["jvmMain"].assertMatches(
                    kotlinStdlibDependencies,
                    jetbrainsAnnotationDependencies,

                    dependsOnDependency(":/commonMain"),
                    projectArtifactDependency(
                        Regular, ":included-build::included",
                        FilePathRegex(".*/included-build/included/build/libs/included-jvm.jar")
                    )
                )
            }
        }
    }

    @GradleTest
    fun `test - sample1 - assemble and execute`(gradleVersion: GradleVersion) {
        project(
            "mpp-composite-build/sample1",
            gradleVersion,
        ) {
            projectPath.resolve("included-build").addDefaultSettingsToSettingsGradle(gradleVersion)
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
        project(
            "mpp-composite-build/sample1",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .disableKmpIsolatedProjectSupport() // a very old Kotlin is involved in this test
                .suppressDeprecationWarningsOn(
                    reason = "KGP 1.7.21 produces deprecation warnings with Gradle 8.4"
                ) { gradleVersion >= GradleVersion.version(TestVersions.Gradle.G_8_4) }
        ) {
            projectPath.resolve("included-build").addDefaultSettingsToSettingsGradle(gradleVersion)
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
    fun `test - sample2-withHostSpecificTargets - import cinterop dependencies`(gradleVersion: GradleVersion) {
        val producer = project("mpp-composite-build/sample2-withHostSpecificTargets/producerBuild", gradleVersion) {
            if (HostManager.hostIsMac) {
                subProject("producerA").buildGradleKts.append(
                    """                      
                        tasks.configureEach {
                            if (name == "iosArm64MetadataJar" || name == "iosX64MetadataJar") {
                                enabled = false
                            }
                        }
                    """.trimIndent()
                )
            }
        }

        project("mpp-composite-build/sample2-withHostSpecificTargets/consumerBuild", gradleVersion) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.toUri().path)
            gradleProperties.append("kotlin.mpp.enableCInteropCommonization=true")

            build("cleanNativeDistributionCommonization")
            build(":consumerA:transformNativeMainCInteropDependenciesMetadataForIde") {
                assertTasksAreNotInTaskGraph(
                    ":producerBuild:producerA:iosArm64MetadataJar",
                    ":producerBuild:producerA:iosX64MetadataJar",
                )
                assertTasksExecuted(":consumerA:transformNativeMainCInteropDependenciesMetadataForIde")

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
                        Regular, ":producerBuild::producerA", FilePathRegex(".*producerA/build/libs/producerA-1.0.0-SNAPSHOT.jar")
                    )
                )

                dependencies["jvmMain"].assertMatches(
                    kotlinStdlibDependencies,
                    jetbrainsAnnotationDependencies,
                    dependsOnDependency(":consumerA/commonMain"),
                    projectArtifactDependency(
                        Regular, ":producerBuild::producerA", FilePathRegex(".*producerA/build/libs/producerA-1.0.0-SNAPSHOT.jar")
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
                    regularSourceDependency(":producerBuild::producerA/commonMain"),
                    kotlinStdlibDependencies
                )

                dependencies["nativeMain"].assertMatches(
                    dependsOnDependency(":consumerA/commonMain"),
                    regularSourceDependency(":producerBuild::producerA/commonMain"),
                    regularSourceDependency(":producerBuild::producerA/nativeMain"),
                    regularSourceDependency(":producerBuild::producerA/linuxMain"),
                    kotlinNativeDistributionDependencies,
                )

                dependencies["linuxMain"].assertMatches(
                    dependencies["nativeMain"],
                    dependsOnDependency(":consumerA/nativeMain"),
                )

                dependencies["linuxX64Main"].assertMatches(
                    dependsOnDependency(":consumerA/commonMain"),
                    dependsOnDependency(":consumerA/nativeMain"),
                    dependsOnDependency(":consumerA/linuxMain"),
                    projectArtifactDependency(
                        Regular,
                        ":producerBuild::producerA",
                        FilePathRegex(".*/linuxX64/main/klib/producerA")
                    ),
                    kotlinNativeDistributionDependencies,
                )
            }
        }
    }

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

            build(
                ":consumerA:compileCommonMainKotlinMetadata",
                buildOptions = buildOptions.suppressWarningFromAgpWithGradle813(gradleVersion)
            ) {
                assertTasksExecuted(":consumerA:compileCommonMainKotlinMetadata")
            }

            build(":consumerB:compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":consumerB:compileCommonMainKotlinMetadata")
            }

            build(":consumerA:resolveIdeDependencies") {
                consumerA.readIdeDependencies()["commonMain"].assertMatches(
                    regularSourceDependency(":producer::/commonMain"),
                    kotlinStdlibDependencies
                )
            }

            build(":consumerB:resolveIdeDependencies") {
                consumerB.readIdeDependencies()["commonMain"].assertMatches(
                    regularSourceDependency(":producer::/commonMain"),
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

    @GradleTest
    fun `test sample7`(gradleVersion: GradleVersion) {
        val producer = project("mpp-composite-build/sample7-KT-59863-pluginManagement.includeBuild/producerBuild", gradleVersion)
        project(
            "mpp-composite-build/sample7-KT-59863-pluginManagement.includeBuild/consumerBuild",
            gradleVersion,
            defaultBuildOptions
        ) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.toUri().path)
            build("projects")
        }
    }

    @TestMetadata("mpp-composite-build/kt65315_with_resources_in_metadata_klib")
    @GradleTest
    fun `KT-65315 composite project with resources in metadata klib`(gradleVersion: GradleVersion) {
        val defaultKotlinNativeVersion = defaultBuildOptions.nativeOptions.version
        val producerKotlinVersion = "1.9.23" // In this version resources were published inside metadata klibs

        val buildOptions = defaultBuildOptions.copy(
            nativeOptions = defaultBuildOptions.nativeOptions.copy(
                version = null,
                disableKlibsCrossCompilation = true
            )
        )

        val producer = project("mpp-composite-build/kt65315_with_resources_in_metadata_klib/producer", gradleVersion) {
            settingsGradleKts.modify {
                it.replace("kotlin_version", "old_kotlin_version")
            }
            gradleProperties.appendText(
                """
                old_kotlin_version=$producerKotlinVersion
                kotlin.native.version=$producerKotlinVersion
                """.trimIndent()
            )
        }

        project(
            "mpp-composite-build/kt65315_with_resources_in_metadata_klib/consumer",
            gradleVersion,
            buildOptions = buildOptions
                .disableKmpIsolatedProjectSupport() // old version of kotlin is involved in this test
                .suppressWarningForOldKotlinVersion(gradleVersion),
        ) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.toUri().path)
            defaultKotlinNativeVersion?.let { gradleProperties.appendText("\nkotlin.native.version=$it") }

            build(":consumerA:assemble") {
                // Check that producer has resources in its metadata
                val allMetadataJar = producer.projectPath.resolve("producerA/build/libs/producerA-metadata-1.0.0-SNAPSHOT.jar")
                allMetadataJar.assertZipFileContains(listOf("commonMain/toot-toot.txt", "nativeMain/toot-toot.txt"))
                assertTasksExecuted(":consumerA:compileCommonMainKotlinMetadata")
                assertTasksExecuted(":consumerA:compileNativeMainKotlinMetadata")
                if (OperatingSystem.current().isMacOsX) {
                    // Check that producer has resources in its metadata
                    val hostSpecificMetadataJar =
                        producer.projectPath.resolve("producerA/build/libs/producerA-iosx64-1.0.0-SNAPSHOT-metadata.jar")
                    hostSpecificMetadataJar.assertZipFileContains(listOf("appleMain/toot-toot.txt"))
                    assertTasksExecuted(":consumerA:compileAppleMainKotlinMetadata")
                }
            }
        }
    }

    @GradleTest
    fun `KT-66568 duplicate cinterop libraries in composite build`(gradleVersion: GradleVersion) {
        val localRepoDir = defaultLocalRepo(gradleVersion)
        val lib = project(
            "mpp-composite-build/kt66568_duplicate_cinterop_libraries/lib",
            gradleVersion,
            localRepoDir = localRepoDir
        )

        project("mpp-composite-build/kt66568_duplicate_cinterop_libraries/app", gradleVersion, localRepoDir = localRepoDir) {
            settingsGradleKts.toFile().replaceText("<lib_path>", lib.projectPath.toUri().path)
            build(":lib:publish")
            build(":app:compileLinuxMainKotlinMetadata") {
                assertOutputDoesNotContain("""KLIB resolver.*The same 'unique_name=.*' found in more than one library""".toRegex())
                val arguments = extractNativeCompilerTaskArguments(":app:compileLinuxMainKotlinMetadata")
                assertEquals(
                    1,
                    arguments.countOccurrencesOf("test_lib-cinterop-foo"),
                    "Unexpected number of test_lib-cinterop-foo"
                )
            }
        }
    }

    @TestMetadata("mpp-composite-build/sample0")
    @GradleTest
    fun `test included build of older version works correctly`(gradleVersion: GradleVersion) {
        val defaultKotlinNativeVersion = defaultBuildOptions.nativeOptions.version
        val oldKotlinVersion = "1.9.24"

        val buildOptions = defaultBuildOptions.copy(
            nativeOptions = defaultBuildOptions.nativeOptions.copy(version = null)
        )

        val producer = project(
            "mpp-composite-build/sample0/producerBuild",
            gradleVersion = gradleVersion
        ) {
            settingsGradleKts.modify {
                it.replace("kotlin_version", "old_kotlin_version")
            }
            gradleProperties.appendText("\nold_kotlin_version=$oldKotlinVersion")
            gradleProperties.appendText("\nkotlin.native.version=$oldKotlinVersion")
        }

        project(
            "mpp-composite-build/sample0/consumerBuild",
            gradleVersion,
            buildOptions = buildOptions.suppressWarningForOldKotlinVersion(gradleVersion),
        ) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.toUri().path)
            defaultKotlinNativeVersion?.let { gradleProperties.appendText("\nkotlin.native.version=$it") }

            build("assemble") {
                assertTasksExecuted(":consumerA:compileCommonMainKotlinMetadata")
                assertTasksExecuted(":consumerA:compileNativeMainKotlinMetadata")
            }
        }
    }
}

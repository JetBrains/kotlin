/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency.Type.Regular
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.*
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.kotilnNativeDistributionDependencies
import org.jetbrains.kotlin.gradle.util.kotlinStdlibDependencies
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.resolveIdeDependencies
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.absolutePathString

@MppGradlePluginTests
@DisplayName("Tests for multiplatform with composite builds")
class MppCompositeBuildIT : KGPBaseTest() {
    @GradleTest
    fun `test - simple composite build - ide dependencies`(gradleVersion: GradleVersion) {
        val producer = project("mpp-composite-build/simple/producerBuild", gradleVersion)

        project("mpp-composite-build/simple/consumerBuild", gradleVersion) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.absolutePathString())
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
    fun `test - simple composite build - assemble`(gradleVersion: GradleVersion) {
        val producer = project("mpp-composite-build/simple/producerBuild", gradleVersion)

        project("mpp-composite-build/simple/consumerBuild", gradleVersion) {
            settingsGradleKts.toFile().replaceText("<producer_path>", producer.projectPath.absolutePathString())
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
}

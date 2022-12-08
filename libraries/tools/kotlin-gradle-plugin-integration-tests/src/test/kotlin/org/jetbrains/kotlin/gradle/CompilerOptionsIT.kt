/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

internal class CompilerOptionsIT : KGPBaseTest() {

    // In Gradle 7.3-7.5 'kotlin-dsl' plugin tries to set up freeCompilerArgs in doFirst task action
    @DisplayName("Allows to set kotlinOptions.freeCompilerArgs on task execution with warning")
    @JvmGradlePluginTests
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_7_3,
        maxVersion = TestVersions.Gradle.G_7_5
    )
    @GradleTest
    internal fun compatibleWithKotlinDsl(gradleVersion: GradleVersion) {
        project("buildSrcWithKotlinDslAndKgp", gradleVersion) {
            build("tasks") {
                assertOutputContains("kotlinOptions.freeCompilerArgs were changed on task :compileKotlin execution phase:")
            }
        }
    }

    @DisplayName("Allow to suppress kotlinOptions.freeCompilerArgs on task execution modification warning")
    @JvmGradlePluginTests
    @GradleTest
    internal fun suppressFreeArgsModification(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.appendText(
                """
                |
                |tasks.named("compileKotlin") {
                |    doFirst {
                |        kotlinOptions.freeCompilerArgs += ["-module-name=java"]
                |    }
                |}
                """.trimMargin()
            )

            gradleProperties.appendText(
                """
                |
                |kotlin.options.suppressFreeCompilerArgsModificationWarning=true
                """.trimMargin()
            )

            build("assemble") {
                assertOutputDoesNotContain("kotlinOptions.freeCompilerArgs were changed on task")
            }
        }
    }

    @DisplayName("compiler plugin arguments set via kotlinOptions.freeCompilerArgs on task execution applied properly")
    @JvmGradlePluginTests
    @GradleTest
    @Disabled
    internal fun freeArgsModifiedAtExecutionTimeCorrectly(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.appendText(
                //language=Gradle
                """
                |
                |tasks.named("compileKotlin") {
                |    kotlinOptions.freeCompilerArgs += ["-P", "plugin:blah-blah:blah-blah1=1"]
                |    doFirst {
                |        kotlinOptions.freeCompilerArgs += ["-P", "plugin:blah-blah:blah-blah2=1", "-P", "plugin:blah-blah:blah-blah3=1"]
                |    }
                |}
                """.trimMargin()
            )

            gradleProperties.appendText(
                //language=properties
                """
                |
                |kotlin.options.suppressFreeCompilerArgsModificationWarning=true
                """.trimMargin()
            )

            build("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertOutputContainsExactlyTimes("-P plugin:blah-blah:", 3)
            }
        }
    }

    @DisplayName("compiler plugin arguments set via kotlinOptions.freeCompilerArgs on task execution applied properly in MPP")
    @MppGradlePluginTests
    @GradleTest
    @Disabled
    internal fun freeArgsModifiedAtExecutionTimeCorrectlyMpp(gradleVersion: GradleVersion) {
        project("new-mpp-lib-with-tests", gradleVersion) {
            buildGradle.appendText(
                //language=Gradle
                """
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile).configureEach {
                |    kotlinOptions.freeCompilerArgs += ["-P", "plugin:blah-blah:blah-blah1=1"]
                |    doFirst {
                |        kotlinOptions.freeCompilerArgs += ["-P", "plugin:blah-blah:blah-blah2=1", "-P", "plugin:blah-blah:blah-blah3=1"]
                |    }
                |}
                """.trimMargin()
            )

            gradleProperties.appendText(
                //language=properties
                """
                |
                |kotlin.options.suppressFreeCompilerArgsModificationWarning=true
                |# to enable the :compileKotlinMetadata task
                |kotlin.mpp.enableCompatibilityMetadataVariant=true
                """.trimMargin()
            )

            val compileTasks = listOf(
                "compileKotlinMetadata",
                "compileKotlinJvmWithJava",
                "compileKotlinJvmWithoutJava",
                "compileKotlinJs",
                // we do not allow modifying free args for K/N at execution time
            )
            build(*compileTasks.toTypedArray(), buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertOutputContainsExactlyTimes("-P plugin:blah-blah:", 3 * compileTasks.size) // 3 times per task
            }
        }
    }
}
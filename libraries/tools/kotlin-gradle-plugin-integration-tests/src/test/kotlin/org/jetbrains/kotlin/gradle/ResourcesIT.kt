/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.*

@DisplayName("Handles resources correctly")
class ResourcesIT : KGPBaseTest() {

    @JvmGradlePluginTests
    @DisplayName("KT-36904: Adding resources to Kotlin source set should work")
    @GradleTest
    internal fun addResourcesKotlinSourceSet(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            val mainResDir = projectPath.resolve("src/main/resources").apply { createDirectories() }
            val mainResFile = mainResDir.resolve("main.txt").apply { writeText("Yay, Kotlin!") }

            val additionalResDir = projectPath.resolve("additionalRes").apply { createDirectory() }
            val additionalResFile = additionalResDir.resolve("test.txt").apply { writeText("Kotlin!") }

            buildGradle.appendText(
                //language=groovy
                """
                |
                |kotlin {
                |    sourceSets.main.resources.srcDir("additionalRes")
                |}
                |
                |tasks.named("jar", Jar) {
                |   duplicatesStrategy = DuplicatesStrategy.INCLUDE
                |}
                """.trimMargin()
            )

            build("jar") {
                assertFileInProjectExists("build/libs/simpleProject.jar")
                projectPath.resolve("build/libs/simpleProject.jar").assertZipArchiveContainsFilesOnce(
                    listOf(mainResFile.name, additionalResFile.name)
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("KT-53402: ignore non project source changes")
    @GradleTest
    fun ignoreNonProjectSourceChanges(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            val resources = projectPath.resolve("src/main/resources").createDirectories()
            val resourceKts = resources.resolve("resource.kts").createFile()
            resourceKts.appendText("lkdfjgkjs invalid something")
            build("assemble")
            resourceKts.appendText("kajhgfkh invalid something")
            build("assemble")
        }
    }
    
    @GradleTest
    @DisplayName("KT-60459: should not overwrite custom resource directories")
    @JvmGradlePluginTests
    fun notOverwriteCustomResDirs(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            val mainResDir = projectPath.resolve("src/main/resources").apply { createDirectories() }
            val mainResFile = mainResDir.resolve("main.txt").apply { writeText("Yay, Kotlin!") }

            val additionalResDir = projectPath.resolve("src/main/extra-resources").apply { createDirectory() }
            val additionalResFile = additionalResDir.resolve("test.txt").apply { writeText("Kotlin!") }

            buildGradle.writeText(
                """
                |plugins {
	            |    id 'java'
	            |    id 'org.jetbrains.kotlin.jvm' apply false
                |}
                |
                |repositories {
                |    mavenLocal()
                |    mavenCentral()
                |}
                |
                |dependencies {
                |   implementation 'com.google.guava:guava:12.0'
                |}
                |
                |tasks.named("jar", Jar) {
                |   duplicatesStrategy = DuplicatesStrategy.INCLUDE
                |}
                |
                |sourceSets.main.resources.srcDir "src/main/extra-resources"
                |
                |apply plugin: 'org.jetbrains.kotlin.jvm'
                |
                """.trimMargin()
            )

            build("jar", forceOutput = true) {
                assertFileInProjectExists("build/libs/simpleProject.jar")
                projectPath.resolve("build/libs/simpleProject.jar").assertZipArchiveContainsFilesOnce(
                    listOf(mainResFile.name, additionalResFile.name)
                )
            }
        }
    }
}
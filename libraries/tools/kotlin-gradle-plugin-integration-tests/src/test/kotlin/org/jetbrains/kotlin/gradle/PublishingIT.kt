/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.test.assertTrue

@DisplayName("Artifacts publication")
@JvmGradlePluginTests
class PublishingIT : KGPBaseTest() {

    private val String.fullProjectName get() = "publishing/$this"

    @DisplayName("Should allow to publish library in project which is using BOM (KT-47444)")
    @GradleTest
    internal fun shouldPublishCorrectlyWithOmittedVersion(gradleVersion: GradleVersion) {
        project("withBom".fullProjectName, gradleVersion) {
            build("publishToMavenLocal")
        }
    }

    @DisplayName("Publishes Kotlin api dependencies as compile")
    @GradleTest
    fun testKotlinJvmProjectPublishesKotlinApiDependenciesAsCompile(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.appendText(
                //language=Groovy
                """
                
                dependencies {
                    api 'org.jetbrains.kotlin:kotlin-reflect'
                }
                
                plugins.apply('maven-publish')
                
                group "com.example"
                version "1.0"
                publishing {
                    repositories { maven { url file("${'$'}buildDir/repo").toURI() } }
                    publications { maven(MavenPublication) { from components.java } }
                }
                """.trimIndent()
            )

            build("publish") {
                val pomText = projectPath
                    .resolve("build/repo/com/example/simpleProject/1.0/simpleProject-1.0.pom")
                    .readText()
                    .replace("\\s+|\\n".toRegex(), "")
                assertTrue {
                    pomText.contains(
                        "<groupId>org.jetbrains.kotlin</groupId>" +
                                "<artifactId>kotlin-reflect</artifactId>" +
                                "<version>${buildOptions.kotlinVersion}</version>" +
                                "<scope>compile</scope>"
                    )
                }
            }
        }
    }

    @DisplayName("Publishing includes stdlib version")
    @GradleTest
    fun testOmittedStdlibVersion(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            buildGradle.appendText(
                //language=Groovy
                """

                plugins.apply('maven-publish')
                            
                group = "com.example"
                version = "1.0"
                
                publishing {
                    publications {
                       myLibrary(MavenPublication) {
                           from components.kotlin
                       }
                    }
                    repositories {
                        maven {
                            url = "${'$'}buildDir/repo"
                        }
                    }
                }
                """.trimIndent()
            )

            build(
                "build",
                "publishAllPublicationsToMavenRepository",
            ) {
                assertTasksExecuted(":compileKotlin", ":compileTestKotlin")
                val pomLines = projectPath.resolve("build/publications/myLibrary/pom-default.xml").readLines()
                val stdlibVersionLineNumber = pomLines.indexOfFirst { "<artifactId>kotlin-stdlib</artifactId>" in it } + 1
                val versionLine = pomLines[stdlibVersionLineNumber]
                assertTrue { "<version>${buildOptions.kotlinVersion}</version>" in versionLine }
            }
        }
    }

    @DisplayName("Publication with old 'maven' plugin is working")
    @GradleTest
    @GradleTestVersions(maxVersion = TestVersions.Gradle.G_6_9)
    @DisabledOnOs(OS.WINDOWS)
    fun testOldMavenPublishing(
        gradleVersion: GradleVersion,
        @TempDir localRepoDir: Path
    ) {
        project(
            projectName = "old-maven-publish",
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
            buildOptions = defaultBuildOptions.copy(
                warningMode = WarningMode.Summary // 'maven' is deprecated
            )
        ) {
            build("uploadArchives") {
                val publishingDir = localRepoDir.resolve("org.jetbrains.kotlin.example").resolve("1.0.0")
                assertDirectoryExists(publishingDir)
                assertFileExists(publishingDir.resolve("org.jetbrains.kotlin.example-1.0.0.jar"))
                val pomFile = publishingDir.resolve("org.jetbrains.kotlin.example-1.0.0.pom")
                assertFileExists(pomFile)
                assertFileContains(
                    pomFile,
                    """
                    |  <dependencies>
                    |    <dependency>
                    |      <groupId>org.jetbrains.kotlin</groupId>
                    |      <artifactId>kotlin-stdlib</artifactId>
                    |      <version>${buildOptions.kotlinVersion}</version>
                    |      <scope>compile</scope>
                    |    </dependency>
                    |  </dependencies>
                    """.trimMargin()
                )
            }
        }
    }

}

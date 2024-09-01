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
}

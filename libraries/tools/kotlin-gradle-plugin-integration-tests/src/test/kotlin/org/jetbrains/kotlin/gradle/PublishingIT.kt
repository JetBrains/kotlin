/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinPublishingDsl
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.addDefaultSettingsToSettingsGradle
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertTrue
import kotlin.test.assertEquals

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

    @DisplayName("KT-69974: pom rewriting with substitutions and included builds")
    @TestMetadata("pom-rewriter")
    @GradleTest
    fun testPomRewriter(gradleVersion: GradleVersion) {
        val localRepo = defaultLocalRepo(gradleVersion)
        project(
            "pom-rewriter",
            gradleVersion,
            localRepoDir = localRepo,
        ) {

            projectPath.resolve("included").addDefaultSettingsToSettingsGradle(
                gradleVersion,
                DependencyManagement.DefaultDependencyManagement(),
                localRepo,
                true
            )

            build("publishJvmPublicationToCustomRepository") {
                val actualPomContent = localRepo.resolve("pom-rewriter")
                    .resolve("pom-rewriter-root-jvm")
                    .resolve("1.0.0")
                    .resolve("pom-rewriter-root-jvm-1.0.0.pom")
                    .readText()
                    .replace(buildOptions.kotlinVersion, "{kotlin_version}")

                val expectedPomFile = projectPath.resolve("expected-pom.xml").toFile()

                assertEqualsToFile(expectedPomFile, actualPomContent)
            }
        }
    }

    @GradleTest
    fun testAdhocSoftwareComponentInKotlinJvm(gradleVersion: GradleVersion) = testAdhocSoftwareComponent(
        gradleVersion = gradleVersion,
        projectDir = "base-kotlin-jvm-library",
        configureProducerProject = {
            // this code is needed to avoid empty artifacts publication that causes runtime error
            kotlinSourcesDir("main")
                .also { it.createDirectories() }
                .resolve("Lib.kt")
                .writeText("class Foo")

            buildScriptInjection {
                val javaComponent = project.components.getByName("java")
                publishing.publications.create("kotlin", MavenPublication::class.java).from(javaComponent)
            }
        },
        configureConsumerProject = {}
    )

    @GradleTest
    fun testAdhocSoftwareComponentInKotlinMultiplatform(gradleVersion: GradleVersion) = testAdhocSoftwareComponent(
        gradleVersion = gradleVersion,
        projectDir = "base-kotlin-multiplatform-library",
        configureProducerProject = {
            buildScriptInjection {
                kotlinMultiplatform.jvm()
                kotlinMultiplatform.linuxX64()
            }

            // this code is needed to avoid empty artifacts publication that causes runtime error
            kotlinSourcesDir("commonMain")
                .also { it.createDirectories() }
                .resolve("Lib.kt")
                .writeText("class Foo")
        },
        configureConsumerProject = {
            buildScriptInjection {
                kotlinMultiplatform.jvm()
                kotlinMultiplatform.linuxX64()
            }
        }
    )

    private fun testAdhocSoftwareComponent(
        gradleVersion: GradleVersion,
        projectDir: String,
        configureProducerProject: TestProject.() -> Unit,
        configureConsumerProject: TestProject.() -> Unit,
    ) {
        val localRepo = defaultLocalRepo(gradleVersion).absolutePathString()
        fun GradleProjectBuildScriptInjectionContext.publishingDsl() = project.extensions.getByName("kotlin") as KotlinPublishingDsl

        project(projectDir, gradleVersion = gradleVersion) {
            settingsGradleKts.appendText("\nrootProject.name = \"kotlin-lib\"")

            buildScriptInjection {
                applyMavenPublishPlugin(File(localRepo))

                project.group = "group"
                project.version = "1.0"

                val customArtifact = project.layout.buildDirectory.asFile.get()
                    .also { it.mkdirs() }
                    .resolve("customArtifact.txt")
                    .also { it.writeText("customArtifactContent") }

                val customAttribute = Attribute.of("myCustomAttribute", String::class.java)
                val myConfiguration = project.configurations.create("myConsumableConfiguration") {
                    it.isCanBeConsumed = true
                    it.isCanBeResolved = false

                    it.attributes.attribute(
                        Category.CATEGORY_ATTRIBUTE,
                        project.objects.named(Category::class.java, Category.DOCUMENTATION)
                    )
                    it.attributes.attribute(
                        customAttribute,
                        "customValue"
                    )
                }
                project.artifacts.add(myConfiguration.name, customArtifact)

                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                publishingDsl().publishing.adhocSoftwareComponent.addVariantsFromConfiguration(myConfiguration) {}
            }

            configureProducerProject()

            build("publish") {}
        }

        val consumer = project(projectDir, gradleVersion = gradleVersion) {
            buildScriptInjection {
                project.repositories.maven {
                    it.setUrl(localRepo)
                }

                val customAttribute = Attribute.of("myCustomAttribute", String::class.java)
                val myResolvableConfiguration = project.configurations.create("myResolvableConfiguration") {
                    it.isCanBeConsumed = false
                    it.isCanBeResolved = true

                    it.attributes.attribute(
                        Category.CATEGORY_ATTRIBUTE,
                        project.objects.named(Category::class.java, Category.DOCUMENTATION)
                    )
                    it.attributes.attribute(
                        customAttribute,
                        "customValue"
                    )
                }
                project.dependencies.add(myResolvableConfiguration.name, "group:kotlin-lib:1.0")
            }

            configureConsumerProject()
        }

        consumer.build("dependencies") {
            assertOutputDoesNotContain("FAILED")
        }

        consumer.build("dependencyInsight", "--dependency", "group:kotlin-lib:1.0", "--configuration", "myResolvableConfiguration") {
            assertOutputContains("Variant myConsumableConfiguration")
        }
    }
}
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
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.MavenModule
import org.jetbrains.kotlin.gradle.util.parsePom
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.uklibs.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals

@DisplayName("Artifacts publication")
@JvmGradlePluginTests
class PublishingIT : KGPBaseTest() {

    @DisplayName("Test api dependencies publish as compile and implementation as runtime; unspecified versions and dependency constraints publish in dependencyManagement section")
    @GradleTest
    fun testKotlinJvmPomPublication(gradleVersion: GradleVersion) {
        val guavaGroup = "com.google.guava"
        val guavaArtifact = "guava"
        val guavaVersion = "12.0"

        val jacksonGroup = "com.fasterxml.jackson"
        val jacksonBomArtifact = "jackson-bom"
        val jacksonVersion = "2.18.1"

        val kotlinReflectArtifact = "kotlin-reflect"

        val implementationConstraint = "constrainImplementation"
        val apiConstraint = "constrainApi"

        val project = project("empty", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()
            transferDependencyResolutionRepositoriesIntoProjectRepositories()
            buildScriptInjection {
                project.plugins.apply("java-library")
                project.plugins.apply("org.jetbrains.kotlin.jvm")

                kotlinJvm.apply {
                    jvmToolchain(8)

                    project.dependencies.constraints.add("implementation", "${implementationConstraint}:${implementationConstraint}:1.0")
                    project.dependencies.constraints.add("api", "${apiConstraint}:${apiConstraint}:1.0")

                    project.configurations.getByName("implementation").dependencies.add(
                        dependencies.platform("${jacksonGroup}:${jacksonBomArtifact}:${jacksonVersion}")
                    )
                    project.configurations.getByName("implementation").dependencies.add(
                        dependencies.create("${guavaGroup}:${guavaArtifact}:${guavaVersion}")
                    )
                    project.configurations.getByName("testImplementation").dependencies.add(
                        dependencies.create("org.testng:testng:6.8")
                    )

                    project.configurations.getByName("api").dependencies.add(
                        dependencies.create("${KOTLIN_MODULE_GROUP}:${kotlinReflectArtifact}")
                    )
                }
            }
        }

        val pom = parsePom(project.publishJava(PublisherConfiguration()).rootComponent.pom)
        assertEquals(
            setOf(
                MavenModule(
                    groupId = KOTLIN_MODULE_GROUP,
                    artifactId = "kotlin-stdlib",
                    version = project.buildOptions.kotlinVersion,
                    scope = "compile",
                ),
                MavenModule(
                    groupId = KOTLIN_MODULE_GROUP,
                    artifactId = kotlinReflectArtifact,
                    version = null,
                    scope = "compile",
                ),
                MavenModule(
                    groupId = guavaGroup,
                    artifactId = guavaArtifact,
                    version = guavaVersion,
                    scope = "runtime",
                ),
            ),
            pom.dependencies().toSet(),
        )
        assertEquals(
            setOf(
                MavenModule(
                    groupId = jacksonGroup,
                    artifactId = jacksonBomArtifact,
                    version = jacksonVersion,
                    scope = "import"
                ),
                MavenModule(
                    groupId = KOTLIN_MODULE_GROUP,
                    artifactId = kotlinReflectArtifact,
                    version = project.buildOptions.kotlinVersion,
                    scope = null,
                ),
                MavenModule(
                    groupId = implementationConstraint,
                    artifactId = implementationConstraint,
                    version = "1.0",
                    scope = null,
                ),
                MavenModule(
                    groupId = apiConstraint,
                    artifactId = apiConstraint,
                    version = "1.0",
                    scope = null,
                ),
            ),
            pom.dependencyManagementConstraints().toSet(),
        )
    }

    @DisplayName("Publishing includes stdlib version")
    @GradleTest
    fun testOmittedStdlibVersion(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.plugins.apply("org.jetbrains.kotlin.jvm")
                project.plugins.apply("maven-publish")
                kotlinJvm.apply {
                    sourceSets.getByName("main").compileSource("class Stub")
                }

                publishing.apply {
                    publications.create("myLibrary", MavenPublication::class.java).from(
                        project.components.named("kotlin").get()
                    )
                }
            }

            val pom = parsePom(publish(PublisherConfiguration()).rootComponent.pom)
            assertEquals(
                setOf(
                    MavenModule(
                        groupId = KOTLIN_MODULE_GROUP,
                        artifactId = "kotlin-stdlib",
                        version = buildOptions.kotlinVersion,
                        scope = "compile",
                    ),
                ),
                pom.dependencies().toSet(),
            )
        }
    }

    @DisplayName("KT-69974: pom rewriting with substitutions and included builds")
    @GradleTest
    fun testPomRewriter(gradleVersion: GradleVersion) {
        val includedName = "included"
        val localName = "local"
        val customPublicationName = "custom-publication"
        val pomRewriterRootName = "pom-rewriter-root"

        val local = project("empty", gradleVersion) {
            buildScriptInjection {
                project.version = "1.2.3"
                project.plugins.apply("maven-publish")
                project.applyMultiplatform {
                    jvm()
                }
            }
        }

        val included = project("empty", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()
            settingsBuildScriptInjection {
                settings.rootProject.name = includedName
            }
            buildScriptInjection {
                project.plugins.apply("maven-publish")
                project.group = "test"
                project.version = "3.2.2"
                project.applyMultiplatform {
                    jvm()
                }
            }
        }

        val customPublication = project("empty", gradleVersion) {
            buildScriptInjection {
                project.plugins.apply("maven-publish")
                project.applyMultiplatform {
                    jvm()
                    macosArm64()
                }

                publishing.apply {
                    publications.named("jvm", MavenPublication::class.java).configure {
                        it.groupId = "fake-group"
                        it.artifactId = "fake-id"
                        it.version = "fake-version"
                    }
                }
            }
        }

        val rootProject = project("empty", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()
            includeBuild(included)
            include(local, localName)
            include(customPublication, customPublicationName)

            settingsBuildScriptInjection {
                settings.rootProject.name = pomRewriterRootName
            }

            buildScriptInjection {
                project.plugins.apply("maven-publish")
                project.group = pomRewriterRootName
                project.applyMultiplatform {
                    jvm()
                    sourceSets.commonMain.dependencies {
                        api("test:included:1.0")
                        api("test:substituted:1.0")
                        api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                        api(project.dependencies.platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))
                        api("org.springframework.boot:spring-boot-starter-web")
                        implementation("test:custom-substituted:1.0")
                    }
                }

                project.configurations.all {
                    it.resolutionStrategy {
                        it.dependencySubstitution {
                            it.substitute(it.module("test:substituted")).using(it.project(":${localName}"))
                            it.substitute(it.module("test:custom-substituted")).using(it.project(":${customPublicationName}"))
                        }
                    }
                }
            }
        }

        val published = rootProject.publish(
            PublisherConfiguration(
                group = pomRewriterRootName,
                version = "1.0"
            )
        )
        val jvmComponentPom = parsePom(published.jvmMultiplatformComponent.pom)

        assertEquals(
            MavenModule(
                groupId = pomRewriterRootName,
                artifactId = "$pomRewriterRootName-jvm",
                version = "1.0",
                scope = null,
            ),
            jvmComponentPom.selfReference
        )

        assertEquals(
            setOf(
                MavenModule(groupId = "test", artifactId = "included-jvm", version = "3.2.2", scope = "compile"),
                MavenModule(groupId = "pom-rewriter-root", artifactId = "local-jvm", version = "1.2.3", scope = "compile"),
                MavenModule(
                    groupId = "org.jetbrains.kotlinx",
                    artifactId = "kotlinx-coroutines-core-jvm",
                    version = "1.8.1",
                    scope = "compile"
                ),
                MavenModule(
                    groupId = "org.springframework.boot",
                    artifactId = "spring-boot-starter-web",
                    version = null,
                    scope = "compile"
                ),
                MavenModule(
                    groupId = "org.jetbrains.kotlin",
                    artifactId = "kotlin-stdlib",
                    version = rootProject.buildOptions.kotlinVersion,
                    scope = "compile"
                ),
                MavenModule(
                    groupId = "fake-group",
                    artifactId = "fake-id",
                    version = "fake-version",
                    scope = "runtime"
                ),
            ),
            jvmComponentPom.dependencies().toSet()
        )

        assertEquals(
            setOf(
                MavenModule(
                    groupId = "org.springframework.boot",
                    artifactId = "spring-boot-dependencies",
                    version = "3.4.0",
                    scope = "import"
                )
            ),
            jvmComponentPom.dependencyManagementConstraints().toSet()
        )
    }

    @GradleTest
    fun testAdhocSoftwareComponentInKotlinJvm(gradleVersion: GradleVersion) = testAdhocSoftwareComponent(
        gradleVersion = gradleVersion,
        projectDir = "base-kotlin-jvm-library",
        configureProducerProject = {
            buildScriptInjection {
                project.plugins.apply("maven-publish")
                // this code is needed to avoid empty artifacts publication that causes runtime error
                kotlinJvm.sourceSets.getByName("main").compileSource("class Foo")

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
                // this code is needed to avoid empty artifacts publication that causes runtime error
                kotlinMultiplatform.sourceSets.getByName("commonMain").compileSource("class Foo")
            }
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
        fun GradleProjectBuildScriptInjectionContext.publishingDsl() = project.extensions.getByName("kotlin") as KotlinPublishingDsl

        val producer = project(projectDir, gradleVersion = gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "kotlin-lib"
            }

            buildScriptInjection {
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
        }.publish(PublisherConfiguration())

        val consumer = project(projectDir, gradleVersion = gradleVersion) {
            addPublishedProjectToRepositories(producer)
            buildScriptInjection {
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
                project.dependencies.add(myResolvableConfiguration.name, producer.rootCoordinate)
            }

            configureConsumerProject()
        }

        consumer.build("dependencies") {
            assertOutputDoesNotContain("FAILED")
        }

        consumer.build("dependencyInsight", "--dependency", producer.rootCoordinate, "--configuration", "myResolvableConfiguration") {
            assertOutputContains("Variant myConsumableConfiguration")
        }
    }
}
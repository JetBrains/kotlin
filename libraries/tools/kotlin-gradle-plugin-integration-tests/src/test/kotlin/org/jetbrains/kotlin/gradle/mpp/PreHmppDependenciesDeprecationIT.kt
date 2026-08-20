package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.PublishedProject
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.uklibs.publish
import kotlin.io.path.writeText

@MppGradlePluginTests
class PreHmppDependenciesDeprecationIT : KGPBaseTest() {

    @GradleTest
    fun testSimpleReport(gradleVersion: GradleVersion) {
        val consumer = project("empty", gradleVersion) {
            val preHmppLibrary = publishPreHmppLibrary(gradleVersion)
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    linuxX64()

                    sourceSets.commonMain.dependencies {
                        implementation(preHmppLibrary.rootCoordinate)
                    }
                }
            }
        }

        consumer.checkDiagnostics(expectReportForDependency = true)
    }

    @GradleTest
    fun testReportFromIntermediateSourceSet(gradleVersion: GradleVersion) {
        val consumer = project("empty", gradleVersion) {
            val preHmppLibrary = publishPreHmppLibrary(gradleVersion)
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    linuxX64()
                    js()

                    val commonMain = sourceSets.getByName("commonMain")
                    val intermediate = sourceSets.create("intermediate") {
                        it.dependsOn(commonMain)
                        it.dependencies {
                            implementation(preHmppLibrary.rootCoordinate)
                        }
                    }

                    sourceSets.getByName("jvmMain").dependsOn(intermediate)
                    sourceSets.getByName("linuxX64Main").dependsOn(intermediate)
                }
            }
        }

        consumer.checkDiagnostics(expectReportForDependency = true)
    }

    @GradleTest
    fun testTransitiveDependencyUpgradesVersion(gradleVersion: GradleVersion) {
        val consumer = project("empty", gradleVersion) {
            // 0.1
            val preHmppLibrary01 = publishPreHmppLibrary(gradleVersion, version = "0.1")

            // 0.2 -- still pre-HMPP
            val preHmppLibrary02 = publishPreHmppLibrary(gradleVersion, version = "0.2")
            val hmppLibrary02 = publishHmppLibraryWithPreHmppInDependencies(
                gradleVersion,
                version = "0.2",
                preHmppLibrary = preHmppLibrary02,
            )
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    linuxX64()

                    sourceSets.commonMain.dependencies {
                        implementation(hmppLibrary02.rootCoordinate)
                        implementation(preHmppLibrary01.rootCoordinate)
                    }
                }
            }
        }

        // Check that even though the version of requested dependency is different from resolved, the report warning is still emitted
        consumer.checkDiagnostics(expectReportForDependency = true)
    }

    @GradleTest
    fun noReportFromTransitiveDependencies(gradleVersion: GradleVersion) {
        val consumer = project("empty", gradleVersion) {
            val preHmppLibrary = publishPreHmppLibrary(gradleVersion)
            val hmppLibrary = publishHmppLibraryWithPreHmppInDependencies(
                gradleVersion,
                version = "0.1",
                preHmppLibrary = preHmppLibrary,
            )
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    linuxX64()

                    sourceSets.commonMain.dependencies {
                        implementation(hmppLibrary.rootCoordinate)
                    }
                }
            }
        }

        consumer.checkDiagnostics(expectReportForDependency = false)
    }

    @GradleTest
    fun noReportWhenSuppressed(gradleVersion: GradleVersion) {
        val consumer = project("empty", gradleVersion) {
            val preHmppLibrary = publishPreHmppLibrary(gradleVersion)
            plugins {
                kotlin("multiplatform")
            }
            gradleProperties.writeText("kotlin.mpp.allow.legacy.dependencies=true")
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    linuxX64()

                    sourceSets.commonMain.dependencies {
                        implementation(preHmppLibrary.rootCoordinate)
                    }
                }
            }
        }

        consumer.checkDiagnostics(expectReportForDependency = false)
    }

    @GradleTest
    fun testNoWarningsOnPopularDependencies(gradleVersion: GradleVersion) {
        val consumer = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm {
                        testRuns.named("test") {
                            it.executionTask.configure { task ->
                                task.useJUnitPlatform()
                            }
                        }
                    }
                    linuxX64()

                    sourceSets.commonMain.dependencies {
                        implementation(kotlin("stdlib"))
                        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                    }

                    sourceSets.commonTest.dependencies {
                        implementation(kotlin("test"))
                    }
                }
            }
        }

        consumer.checkDiagnostics(expectReportForDependency = false)
    }

    @GradleTest
    fun testNoWarningsOnKotlinTestIfAddedInCommonMain(gradleVersion: GradleVersion) {
        val consumer = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    linuxX64()

                    sourceSets.commonMain.dependencies {
                        implementation(kotlin("test"))
                    }

                    sourceSets.getByName("jvmMain").dependencies {
                        implementation(kotlin("test-junit"))
                    }
                }
            }
        }

        consumer.checkDiagnostics(expectReportForDependency = false)
    }

    @GradleTest
    fun testNoWarningsOnProjectDependencies(gradleVersion: GradleVersion) {
        val root = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform").apply(false)
            }

            val producer = project("empty", gradleVersion) {
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        jvm()
                        js()
                        linuxX64()
                    }
                }
            }

            val consumer = project("empty", gradleVersion) {
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        jvm()
                        js()
                        linuxX64()

                        sourceSets.commonMain.dependencies {
                            implementation(project(":producer"))
                        }
                    }
                }
            }

            include(producer, "producer")
            include(consumer, "consumer")
        }

        root.checkDiagnostics(
            expectReportForDependency = false,
            projectPathToCheck = ":consumer",
        )
    }

    @GradleTest
    fun testNoWarningsInPlatformSpecificSourceSetsOrTests(gradleVersion: GradleVersion) {
        val consumer = project("empty", gradleVersion) {
            val preHmppLibrary = publishPreHmppLibrary(gradleVersion)
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    js()
                    linuxX64()

                    listOf("jvmMain", "jsMain", "linuxX64Main").forEach {
                        sourceSets.getByName(it).dependencies {
                            implementation(preHmppLibrary.rootCoordinate)
                        }
                    }

                    // see KT-60724
                    sourceSets.commonTest.dependencies {
                        implementation(preHmppLibrary.rootCoordinate)
                    }
                }
            }
        }

        consumer.checkDiagnostics(expectReportForDependency = false)
    }

    private fun TestProject.checkDiagnostics(
        expectReportForDependency: Boolean,
        projectPathToCheck: String = "", // empty means rootProject
    ) {
        build("$projectPathToCheck:dependencies", buildOptions = defaultBuildOptions) {
            // all dependencies should be resolved, Gradle won't fail the 'dependencies' task on its own
            assertOutputDoesNotContain("FAILED")
            if (expectReportForDependency) {
                output.assertHasDiagnostic(
                    KotlinToolingDiagnostics.PreHmppDependenciesUsedInBuild
                )
            } else {
                output.assertNoDiagnostic(KotlinToolingDiagnostics.PreHmppDependenciesUsedInBuild)
            }
        }
    }

    private fun TestProject.publishPreHmppLibrary(
        gradleVersion: GradleVersion,
        version: String = "0.1",
    ): PublishedProject {
        val library = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    js()
                    linuxX64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                // Simulate a dependency published before HMPP was introduced: such libraries published their
                // 'metadata' variant with the legacy 'kotlin-api' usage instead of the modern 'kotlin-metadata' one.
                project.afterEvaluate {
                    project.configurations.getByName("metadataApiElements").attributes.attribute(
                        Usage.USAGE_ATTRIBUTE,
                        project.objects.named(Usage::class.java, "kotlin-api")
                    )
                }
            }
        }

        val published = library.publish(
            publisherConfiguration = PublisherConfiguration(group = "org.jetbrains.kotlin.tests", version = version),
            deriveBuildOptions = { defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899() },
        )
        addPublishedProjectToRepositories(published)
        return published
    }

    private fun TestProject.publishHmppLibraryWithPreHmppInDependencies(
        gradleVersion: GradleVersion,
        version: String,
        preHmppLibrary: PublishedProject,
    ): PublishedProject {
        val library = project("empty", gradleVersion) {
            addPublishedProjectToRepositories(preHmppLibrary)
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    // Note: no 'js()' target here on purpose. Deriving common metadata from a legacy dependency that
                    // lacks a proper metadata variant makes 'transformCommonMainDependenciesMetadata' depend on the
                    // platform compilation task, which for the JS target leads to a self-referencing 'compileKotlinJs'
                    // task cycle. That's a real, pre-existing limitation of consuming pre-HMPP libraries, and it is
                    // orthogonal to what these tests are verifying.
                    jvm()
                    linuxX64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    sourceSets.commonMain.dependencies {
                        api(preHmppLibrary.rootCoordinate)
                    }
                }
            }
        }

        val published = library.publish(
            // Note: distinct group from 'publishPreHmppLibrary' is important here. Every '"empty"' template project
            // shares the same artifact name, so using the same group would make this library depend on an artifact
            // with the exact same coordinates as itself.
            publisherConfiguration = PublisherConfiguration(group = "org.jetbrains.kotlin.tests.hmpp", version = version),
            deriveBuildOptions = { defaultBuildOptions },
        )
        addPublishedProjectToRepositories(published)
        return published
    }
}

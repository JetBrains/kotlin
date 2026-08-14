/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.archive

import org.gradle.api.tasks.Copy
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.ResolvedComponentWithArtifacts
import org.jetbrains.kotlin.gradle.testing.compilationResolution
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.PublishedProject
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.enableCinteropCommonization
import org.jetbrains.kotlin.gradle.uklibs.ignoreAccessViolations
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals

@MppGradlePluginTests
@DisplayName("Consumption of a project published in the Kotlin Archive format")
class KotlinArchiveConsumptionIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    fun consumptionTest(gradleVersion: GradleVersion) {
        val publishedProject = kotlinArchiveProducer(gradleVersion).publish()

        val consumer = kotlinArchiveConsumer(gradleVersion, publishedProject)

        consumer.build("assemble") {
            assertTasksExecuted(expectedConsumerCompilationTasks)
        }

        assertEquals(
            expectedResolution(publishedProject).prettyPrinted,
            consumer.resolveCompileDependencies().prettyPrinted,
        )
    }

    private fun expectedResolution(
        publishedProject: PublishedProject,
    ): Map<String, Map<String, ResolvedComponentWithArtifacts>> = mapOf(
        "linuxX64" to mapOf(
            publishedProject.rootCoordinate to ResolvedComponentWithArtifacts(
                artifacts = listOf(
                    mapOf(
                        "artifactType" to "xz",
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "non-jvm",
                        "org.gradle.usage" to "kotlin-api",
                        "org.jetbrains.kotlin.kar.compression.method" to "NONE",
                        "org.jetbrains.kotlin.kar.state" to "PLATFORM_ARTIFACTS_EXTRACTED",
                        "org.jetbrains.kotlin.native.target" to "linux_x64",
                        "org.jetbrains.kotlin.platform.type" to "native",
                    ),
                ),
                configuration = "linuxX64ApiElements-published",
            ),
            "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                artifacts = listOf(),
                configuration = "nativeApiElements",
            ),
        ),
        "jvm" to mapOf(
            publishedProject.rootCoordinate to ResolvedComponentWithArtifacts(
                artifacts = listOf(),
                configuration = "jvmApiElements-published",
            ),
            publishedProject.jvmCoordinate to ResolvedComponentWithArtifacts(
                artifacts = listOf(
                    mapOf(
                        "artifactType" to "jar",
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "standard-jvm",
                        "org.gradle.libraryelements" to "jar",
                        "org.gradle.usage" to "java-api",
                        "org.jetbrains.kotlin.platform.type" to "jvm",
                    ),
                ),
                configuration = "jvmApiElements-published",
            ),
            "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                artifacts = listOf(
                    mapOf(
                        "artifactType" to "jar",
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "standard-jvm",
                        "org.gradle.libraryelements" to "jar",
                        "org.gradle.usage" to "java-api",
                        "org.jetbrains.kotlin.platform.type" to "jvm",
                    ),
                ),
                configuration = "jvmApiElements",
            ),
            "org.jetbrains:annotations:13.0" to ResolvedComponentWithArtifacts(
                artifacts = listOf(
                    mapOf(
                        "artifactType" to "jar",
                        "org.gradle.category" to "library",
                        "org.gradle.libraryelements" to "jar",
                        "org.gradle.usage" to "java-api",
                    ),
                ),
                configuration = "compile",
            ),
        ),
    )

    private val PublishedProject.jvmCoordinate: String
        get() = "$group:$name-jvm:$version"

    @GradleTest
    fun consumptionWithResourcesAndCinteropsTest(gradleVersion: GradleVersion) {
        val publishedProject = kotlinArchiveComplexProducer(gradleVersion).publish()

        val consumer = kotlinArchiveConsumerWithResourcesAndCInterop(gradleVersion, publishedProject)

        consumer.build("assemble", RESOLVE_RESOURCES_TASK_NAME) {
            assertTasksExecuted(expectedConsumerCompilationTasks)
        }

        assertEquals(
            expectedResolvedResources.prettyPrinted,
            consumer.resolvedResources().prettyPrinted,
        )
    }

    /**
     * A consumer of the [kotlinArchiveComplexProducer]: on top of calling the producer declarations, every
     * native source set calls the cinterop function, which is commonized for the shared ones, and all the
     * resources published by the producer are resolved into a single directory, grouped by target.
     */
    private fun kotlinArchiveConsumerWithResourcesAndCInterop(
        gradleVersion: GradleVersion,
        publishedProject: PublishedProject,
    ): TestProject = project("empty", gradleVersion) {
        addKgpToBuildScriptCompilationClasspath()
        addPublishedProjectToRepositories(publishedProject)
        settingsBuildScriptInjection {
            settings.rootProject.name = "consumer"
        }
        visibleProducerDeclarations.forEach { sourceSet ->
            addSourceFile(sourceSet.key, consumerSource(sourceSet.key, sourceSet.value, withCinterops = true))
        }
        buildScriptInjection {
            project.enableCinteropCommonization()
            project.applyMultiplatform {
                kotlinArchiveTargets()

                sourceSets.commonMain.dependencies {
                    implementation(publishedProject.rootCoordinate)
                }

                val resourcesPublication = project.extraProperties.get(
                    KotlinTargetResourcesPublication.EXTENSION_NAME
                ) as KotlinTargetResourcesPublication
                // Resources have to be resolved before the task is registered: resolution registers tasks itself,
                // which is not allowed while another task is being created
                val resolvedResources = targets
                    .filter { resourcesPublication.canResolveResources(it) }
                    .associate { target -> target.name to resourcesPublication.resolveResources(target) }
                project.tasks.register(RESOLVE_RESOURCES_TASK_NAME, Copy::class.java) { copy ->
                    copy.into(project.layout.buildDirectory.dir(RESOLVED_RESOURCES_DIRECTORY))
                    resolvedResources.forEach { target ->
                        copy.from(target.value) { spec -> spec.into(target.key) }
                    }
                }
            }
        }
    }

    /**
     * All the resolved resource files, relative to the directory they are resolved into.
     */
    private fun TestProject.resolvedResources(): List<String> {
        val resolvedResources = projectPath.resolve("build/$RESOLVED_RESOURCES_DIRECTORY").toFile()
        return resolvedResources.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(resolvedResources).invariantSeparatorsPath }
            .sorted()
            .toList()
    }

    private fun consumerSource(
        sourceSetName: String,
        producerDeclarations: List<String>,
        withCinterops: Boolean = false,
    ): String {
        val callsCinterop = withCinterops && sourceSetName in nativeSourceSets
        val functionName = "consumeIn" + sourceSetName.replaceFirstChar { it.uppercase() }
        val calls = producerDeclarations.map { declaration -> "    $declaration()" } +
                if (callsCinterop) listOf("    $PRODUCER_CINTEROP_FUNCTION()") else emptyList()
        val optIn = if (callsCinterop) "@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)\n\n" else ""
        return optIn + "fun $functionName() {\n" + calls.joinToString(separator = "\n") + "\n}"
    }

    private fun TestProject.resolveCompileDependencies(): Map<String, Map<String, ResolvedComponentWithArtifacts>> =
        buildScriptReturn {
            project.ignoreAccessViolations {
                targetsWithCheckedResolution.associateWith { targetName ->
                    kotlinMultiplatform.targets.getByName(targetName).compilationResolution()
                }
            }
        }.buildAndReturn("assemble")

    private fun kotlinArchiveConsumer(
        gradleVersion: GradleVersion,
        publishedProject: PublishedProject,
    ): TestProject = project("empty", gradleVersion) {
        addKgpToBuildScriptCompilationClasspath()
        addPublishedProjectToRepositories(publishedProject)
        settingsBuildScriptInjection {
            settings.rootProject.name = "consumer"
        }
        visibleProducerDeclarations.forEach { sourceSet ->
            addSourceFile(sourceSet.key, consumerSource(sourceSet.key, sourceSet.value))
        }
        buildScriptInjection {
            project.applyMultiplatform {
                kotlinArchiveTargets()

                sourceSets.commonMain.dependencies {
                    implementation(publishedProject.rootCoordinate)
                }
            }
        }
    }

    companion object {
        private val visibleProducerDeclarations: Map<String, List<String>> = mapOf(
            "commonMain" to listOf("commonMain"),
            "nativeMain" to listOf("commonMain", "nativeMain"),
            "appleMain" to listOf("commonMain", "nativeMain", "appleMain"),
            "webMain" to listOf("commonMain", "webMain"),
            "jvmMain" to listOf("commonMain", "jvmMain"),
            "jsMain" to listOf("commonMain", "webMain", "jsMain"),
            "wasmJsMain" to listOf("commonMain", "webMain", "wasmJsMain"),
            "linuxX64Main" to listOf("commonMain", "nativeMain", "linuxX64Main"),
            "iosArm64Main" to listOf("commonMain", "nativeMain", "appleMain", "iosArm64Main"),
            "macosArm64Main" to listOf("commonMain", "nativeMain", "appleMain", "macosArm64Main"),
        )

        private val targetsWithCheckedResolution = listOf("linuxX64", "jvm")

        /**
         * The source sets which can call the cinterop declarations of the producer.
         */
        private val nativeSourceSets = setOf(
            "nativeMain",
            "appleMain",
            "linuxX64Main",
            "iosArm64Main",
            "macosArm64Main",
        )

        private const val RESOLVE_RESOURCES_TASK_NAME = "resolveProducerResources"
        private const val RESOLVED_RESOURCES_DIRECTORY = "resolvedProducerResources"

        /**
         * Resources of every target, merged from all the source sets it is compiled from.
         *
         * Resources are not published for the jvm target, so it resolves none of them.
         */
        private val expectedResolvedResources = listOf(
            "iosArm64/$PRODUCER_RESOURCES_PLACEMENT/commonMain.txt",
            "iosArm64/$PRODUCER_RESOURCES_PLACEMENT/iosArm64Main.txt",
            "iosArm64/$PRODUCER_RESOURCES_PLACEMENT/nativeMain.txt",
            "js/$PRODUCER_RESOURCES_PLACEMENT/commonMain.txt",
            "js/$PRODUCER_RESOURCES_PLACEMENT/jsMain.txt",
            "linuxX64/$PRODUCER_RESOURCES_PLACEMENT/commonMain.txt",
            "linuxX64/$PRODUCER_RESOURCES_PLACEMENT/linuxX64Main.txt",
            "linuxX64/$PRODUCER_RESOURCES_PLACEMENT/nativeMain.txt",
            "macosArm64/$PRODUCER_RESOURCES_PLACEMENT/commonMain.txt",
            "macosArm64/$PRODUCER_RESOURCES_PLACEMENT/macosArm64Main.txt",
            "macosArm64/$PRODUCER_RESOURCES_PLACEMENT/nativeMain.txt",
            "wasmJs/$PRODUCER_RESOURCES_PLACEMENT/commonMain.txt",
            "wasmJs/$PRODUCER_RESOURCES_PLACEMENT/wasmJsMain.txt",
        )

        private val expectedConsumerCompilationTasks = listOf(
            ":compileCommonMainKotlinMetadata",
            ":compileNativeMainKotlinMetadata",
            ":compileAppleMainKotlinMetadata",
            ":compileWebMainKotlinMetadata",
            ":compileKotlinJvm",
            ":compileKotlinJs",
            ":compileKotlinWasmJs",
            ":compileKotlinLinuxX64",
            ":compileKotlinIosArm64",
            ":compileKotlinMacosArm64",
        )
    }
}

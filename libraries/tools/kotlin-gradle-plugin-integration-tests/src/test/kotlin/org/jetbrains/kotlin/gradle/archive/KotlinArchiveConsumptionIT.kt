/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.archive

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.targets.js.npm.AbstractNodeModulesCache
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.assertNoDiagnostic
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
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
import org.junit.jupiter.api.condition.OS
import java.io.File
import kotlin.test.assertEquals
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

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
            assertTasksExecuted(expectedConsumerCompilationTasks())
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
                        "org.jetbrains.kotlin.kar.compression.method" to "none",
                        "org.jetbrains.kotlin.kar.state" to "platform-artifacts-extracted",
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
    fun consumptionWithResourcesTest(gradleVersion: GradleVersion) {
        val producer = kotlinArchiveProducer(gradleVersion)
        producer.configureResourcesPublication()
        val publishedProject = producer.publish()

        val consumer = kotlinArchiveConsumer(gradleVersion, publishedProject)
        consumer.configureResourcesResolution()

        consumer.build("assemble", RESOLVE_RESOURCES_TASK_NAME) {
            assertTasksExecuted(expectedConsumerCompilationTasks() + ":${RESOLVE_RESOURCES_TASK_NAME}")
        }

        assertEquals(
            expectedResolvedResources.prettyPrinted,
            consumer.resolvedResources().prettyPrinted,
        )
    }

    private fun consumptionWithCInteropsTestImpl(gradleVersion: GradleVersion, withAppleTargets: Boolean) {
        val producer = kotlinArchiveProducer(gradleVersion, withAppleTargets)
        producer.configureCinterop()
        val publishedProject = producer.publish()

        val consumer = kotlinArchiveConsumer(gradleVersion, publishedProject, withAppleTargets)
        consumer.cinteropsCallsSource()

        consumer.build("assemble") {
            assertTasksExecuted(expectedConsumerCompilationTasks(withAppleTargets))
            assertNoDiagnostic(KotlinToolingDiagnostics.UnsupportedKotlinArchiveUsage)
        }
    }

    @GradleTest
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    fun consumptionWithCinteropsTest(gradleVersion: GradleVersion) {
        consumptionWithCInteropsTestImpl(gradleVersion, withAppleTargets = true)
    }

    @GradleTest
    @OsCondition(supportedOn = [OS.MAC, OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.LINUX, OS.WINDOWS])
    fun consumptionWithCinteropsWithoutAppleTargetsTest(gradleVersion: GradleVersion) {
        consumptionWithCInteropsTestImpl(gradleVersion, withAppleTargets = false)
    }

    @GradleTest
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    @DisplayName("A framework of a consumer exports a library in the Kotlin Archive format")
    fun consumptionWithFrameworkExportTest(gradleVersion: GradleVersion) {
        val publishedProject = kotlinArchiveProducer(gradleVersion).publish()

        val consumer = kotlinArchiveConsumer(gradleVersion, publishedProject, apiDependency = true)
        consumer.exportProducerFromFrameworks(publishedProject)

        consumer.build(*expectedFrameworkLinkTasks.toTypedArray()) {
            assertTasksExecuted(expectedFrameworkLinkTasks)
            assertNoDiagnostic(KotlinToolingDiagnostics.UnsupportedKotlinArchiveUsage)

            appleTargets.forEach { targetName ->
                assertEquals(
                    // This is not published one, but rather unpacked from klib
                    listOf("${publishedProject.name}-$targetName.klib"),
                    exportedLibraryNames(frameworkLinkTaskOf(targetName)),
                    "Unexpected libraries exported from the framework of $targetName",
                )
            }
        }
    }

    private fun BuildResult.exportedLibraryNames(taskPath: String): List<String> =
        extractNativeCompilerTaskArguments(taskPath)
            .lines()
            .map { it.trim() }
            .filter { it.startsWith(EXPORT_LIBRARY_ARGUMENT) }
            .map { File(it.removePrefix(EXPORT_LIBRARY_ARGUMENT)).name }
            .sorted()

    @GradleTest
    @DisplayName("A consumer with its own cinterop uses a library in the Kotlin Archive format")
    fun consumptionWithConsumerCinteropsTest(gradleVersion: GradleVersion) {
        val publishedProject = kotlinArchiveProducer(gradleVersion, withAppleTargets = false).publish()

        val consumer = kotlinArchiveConsumer(gradleVersion, publishedProject, withAppleTargets = false)
        consumer.configureCinterop(CONSUMER_CINTEROP_NAME, CONSUMER_CINTEROP_PACKAGE)
        consumer.cinteropsCallsSource(CONSUMER_CINTEROP_FUNCTION)

        consumer.build("assemble") {
            assertTasksExecuted(expectedConsumerCompilationTasks(withAppleTargets = false))
            assertNoDiagnostic(KotlinToolingDiagnostics.UnsupportedKotlinArchiveUsage)
        }
    }

    @GradleTest
    @DisplayName("A consumer gets the npm dependencies of a library in the Kotlin Archive format")
    fun consumptionWithNpmDependenciesTest(gradleVersion: GradleVersion) {
        val producer = kotlinArchiveProducer(gradleVersion, withAppleTargets = false, withNodeJs = true)
        producer.buildScriptInjection {
            project.applyMultiplatform {
                sourceSets.jsMain.dependencies {
                    api(npm("test-npm-dep", "1.1.1"))
                }
                sourceSets.wasmJsMain.dependencies {
                    api(npm("test-npm-dep", "1.1.2"))
                }
            }
        }
        val publishedProject = producer.publish()

        val consumer = kotlinArchiveConsumer(gradleVersion, publishedProject, withAppleTargets = false)
        // The npm tasks only exist for a target with an engine
        consumer.buildScriptInjection {
            project.applyMultiplatform {
                js { nodejs() }
                wasmJs { nodejs() }
            }
        }

        consumer.build("jsPackageJson", "wasmJsPackageJson") {
            assertTasksExecuted(":jsPackageJson", ":wasmJsPackageJson")
            assertEquals(
                "1.1.1",
                consumer.mainPackageJsonDependencies("js")["test-npm-dep"]
            )
            assertEquals(
                "1.1.2",
                consumer.mainPackageJsonDependencies("wasm")["test-npm-dep"]
            )
        }
    }

    private fun TestProject.mainPackageJsonDependencies(target: String): Map<String, String> {
        val packageJson = projectPath.resolve("build/$target/packages")
            .listDirectoryEntries()
            .single { !it.name.endsWith("-test") }
            .resolve("package.json")
        assertFileExists(packageJson)
        return Json.parseToJsonElement(packageJson.readText())
            .jsonObject["dependencies"]
            ?.jsonObject
            ?.mapValues { it.value.jsonPrimitive.content }
            .orEmpty()
    }

    @GradleTest
    @DisplayName("A consumer imports the files stored inside the klib of a library in the Kotlin Archive format")
    fun consumptionOfFilesStoredInsideKlibTest(gradleVersion: GradleVersion) {
        val producer = kotlinArchiveProducer(
            gradleVersion,
            withAppleTargets = false,
            withNodeJs = true,
        )
        producer.configureWebResourcesStoredInsideKlib()
        val publishedProject = producer.publish("jsPublicPackageJson", "wasmJsPublicPackageJson")

        val consumer = kotlinArchiveConsumer(gradleVersion, publishedProject, withAppleTargets = false)
        consumer.buildScriptInjection {
            project.applyMultiplatform {
                this.js { nodejs() }
                this.wasmJs { nodejs() }
            }
        }
        consumer.build("jsPackageJson", "wasmJsPackageJson") {
            assertTasksExecuted(":jsPackageJson", ":wasmJsPackageJson")
        }
        val importedNodeModulesList = webRootDirectories.flatMap { rootDirectory ->
            val importedModulesFile = consumer.projectPath.resolve("build/$rootDirectory/packages_imported").toFile()
            importedModulesFile.walkTopDown()
                .filter { it.isFile }
                .map { it.relativeTo(importedModulesFile).invariantSeparatorsPath }
                // The state file of the node modules cache belongs to the build, not to a module
                .filterNot { it.startsWith(AbstractNodeModulesCache.STATE_FILE_NAME) }
                .map { path ->
                    val segments = path.split("/")
                    // A module is stored as <name>/<version>/<its files>
                    "$rootDirectory/${segments.take(2).joinToString("/")}" to segments.drop(2).joinToString("/")
                }
                .toList()
        }
        val importedNodeModules: Map<String, List<String>> = importedNodeModulesList
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.sorted() }
            .toSortedMap()

        assertEquals(
            expectedImportedNodeModules.prettyPrinted,
            importedNodeModules.prettyPrinted,
        )
    }

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
    ): String {
        val functionName = "consumeIn" + sourceSetName.replaceFirstChar { it.uppercase() }
        val calls = producerDeclarations.joinToString(separator = "\n") { declaration -> "    $declaration()" }
        return "fun $functionName() {\n$calls\n}"
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
        withAppleTargets: Boolean = true,
        apiDependency: Boolean = false,
    ): TestProject = project("empty", gradleVersion) {
        plugins { kotlin("multiplatform") }
        addPublishedProjectToRepositories(publishedProject)
        settingsBuildScriptInjection {
            settings.rootProject.name = "consumer"
        }
        visibleProducerDeclarations
            .filterKeys { withAppleTargets || it !in kotlinArchiveAppleSourceSets }
            .forEach { sourceSet ->
                addSourceFile(sourceSet.key, consumerSource(sourceSet.key, sourceSet.value))
            }
        buildScriptInjection {
            project.applyMultiplatform {
                kotlinArchiveTargets(withAppleTargets)

                sourceSets.commonMain.dependencies {
                    if (apiDependency) {
                        api(publishedProject.rootCoordinate)
                    } else {
                        implementation(publishedProject.rootCoordinate)
                    }
                }
            }
        }
    }

    private fun TestProject.exportProducerFromFrameworks(publishedProject: PublishedProject) {
        buildScriptInjection {
            project.applyMultiplatform {
                appleTargets.forEach { targetName ->
                    val target = targets.getByName(targetName) as KotlinNativeTarget
                    target.binaries.framework {
                        export(publishedProject.rootCoordinate)
                    }
                }
            }
        }
    }

    private fun TestProject.configureResourcesResolution() {
        buildScriptInjection {
            project.applyMultiplatform {
                val resourcesPublication = project.extraProperties.get(
                    KotlinTargetResourcesPublication.EXTENSION_NAME
                ) as KotlinTargetResourcesPublication
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

    private fun TestProject.cinteropsCallsSource(cinteropFunction: String = PRODUCER_CINTEROP_FUNCTION) {
        buildScriptInjection {
            project.enableCinteropCommonization()
        }
        nativeSourceSets.forEach { sourceSetName ->
            val functionName = "callCinteropIn" + sourceSetName.replaceFirstChar { it.uppercase() }
            addSourceFile(
                sourceSetName,
                """
                @file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

                fun $functionName() {
                    $cinteropFunction()
                }
                """.trimIndent(),
                fileName = "${sourceSetName}Cinterop.kt",
            )
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
            "linuxMain" to listOf("commonMain", "nativeMain", "linuxMain"),
            "linuxX64Main" to listOf("commonMain", "nativeMain", "linuxMain", "linuxX64Main"),
            "linuxArm64Main" to listOf("commonMain", "nativeMain", "linuxMain", "linuxArm64Main"),
            "iosArm64Main" to listOf("commonMain", "nativeMain", "appleMain", "iosArm64Main"),
            "macosArm64Main" to listOf("commonMain", "nativeMain", "appleMain", "macosArm64Main"),
        )

        private val targetsWithCheckedResolution = listOf("linuxX64", "jvm")

        private val appleTargets = listOf("iosArm64", "macosArm64")

        private val webRootDirectories = listOf("js", "wasm")

        private val expectedImportedNodeModules = mapOf(
            "js/producer/1.0.0" to listOf(
                "package.json",
                "stored.html",
                "stored.js",
                "stored.js.map",
                "stored.mjs",
                "stored.wasm",
            ),
            "wasm/producer/1.0.0" to listOf(
                "package.json",
                "stored.html",
                "stored.js",
                "stored.js.map",
                "stored.mjs",
                "stored.wasm",
            ),
        )

        private fun frameworkLinkTaskOf(targetName: String): String =
            ":linkDebugFramework" + targetName.replaceFirstChar { it.uppercase() }

        private val expectedFrameworkLinkTasks = appleTargets.map(::frameworkLinkTaskOf)

        private const val EXPORT_LIBRARY_ARGUMENT = "-Xexport-library="

        private val nativeSourceSets = setOf(
            "nativeMain",
            "appleMain",
            "linuxMain",
            "linuxX64Main",
            "linuxArm64Main",
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
            "iosArm64/$PRODUCER_RESOURCES_PLACEMENT/appleMain.txt",
            "iosArm64/$PRODUCER_RESOURCES_PLACEMENT/commonMain.txt",
            "iosArm64/$PRODUCER_RESOURCES_PLACEMENT/iosArm64Main.txt",
            "iosArm64/$PRODUCER_RESOURCES_PLACEMENT/nativeMain.txt",
            "js/$PRODUCER_RESOURCES_PLACEMENT/commonMain.txt",
            "js/$PRODUCER_RESOURCES_PLACEMENT/jsMain.txt",
            "js/$PRODUCER_RESOURCES_PLACEMENT/webMain.txt",
            "linuxArm64/$PRODUCER_RESOURCES_PLACEMENT/commonMain.txt",
            "linuxArm64/$PRODUCER_RESOURCES_PLACEMENT/linuxArm64Main.txt",
            "linuxArm64/$PRODUCER_RESOURCES_PLACEMENT/linuxMain.txt",
            "linuxArm64/$PRODUCER_RESOURCES_PLACEMENT/nativeMain.txt",
            "linuxX64/$PRODUCER_RESOURCES_PLACEMENT/commonMain.txt",
            "linuxX64/$PRODUCER_RESOURCES_PLACEMENT/linuxMain.txt",
            "linuxX64/$PRODUCER_RESOURCES_PLACEMENT/linuxX64Main.txt",
            "linuxX64/$PRODUCER_RESOURCES_PLACEMENT/nativeMain.txt",
            "macosArm64/$PRODUCER_RESOURCES_PLACEMENT/appleMain.txt",
            "macosArm64/$PRODUCER_RESOURCES_PLACEMENT/commonMain.txt",
            "macosArm64/$PRODUCER_RESOURCES_PLACEMENT/macosArm64Main.txt",
            "macosArm64/$PRODUCER_RESOURCES_PLACEMENT/nativeMain.txt",
            "wasmJs/$PRODUCER_RESOURCES_PLACEMENT/commonMain.txt",
            "wasmJs/$PRODUCER_RESOURCES_PLACEMENT/wasmJsMain.txt",
            "wasmJs/$PRODUCER_RESOURCES_PLACEMENT/webMain.txt",
        )

        private fun expectedConsumerCompilationTasks(
            withAppleTargets: Boolean = true
        ): List<String> = listOfNotNull(
            ":compileCommonMainKotlinMetadata",
            ":compileNativeMainKotlinMetadata",
            ":compileLinuxMainKotlinMetadata",
            ":compileWebMainKotlinMetadata",
            ":compileKotlinJvm",
            ":compileKotlinJs",
            ":compileKotlinWasmJs",
            ":compileKotlinLinuxX64",
            ":compileKotlinLinuxArm64",
            ":compileAppleMainKotlinMetadata".takeIf { withAppleTargets },
            ":compileKotlinIosArm64".takeIf { withAppleTargets },
            ":compileKotlinMacosArm64".takeIf { withAppleTargets }
        )
    }
}

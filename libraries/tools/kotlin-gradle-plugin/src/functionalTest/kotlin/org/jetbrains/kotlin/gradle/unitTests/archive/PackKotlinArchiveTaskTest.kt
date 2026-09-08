/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.unitTests.archive

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPublicationFormat
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.PackKotlinArchiveTask
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.AssembleKotlinArchiveTask
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableCInteropCommonization
import org.jetbrains.kotlin.gradle.util.enableMppResourcesPublication
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.populateTaskGraph
import org.jetbrains.kotlin.gradle.util.setAndroidSdkDirProperty
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.test.*

class PackKotlinArchiveTaskTest {
    @Test
    fun `assemble task depends on klibs metadata and project structure metadata`() {
        val project = buildProjectWithMPP {
            kotlin {
                // Also register binaries to test, that link tasks are not in task graph
                js {
                    binaries.executable()
                }
                wasmJs {
                    binaries.executable()
                }
                macosArm64 {
                    binaries.executable()
                }
                jvm()
                publishing {
                    publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE)
                }
            }
        }.evaluate()

        val assembleTaskDependencies = project.tasks.getByName("assembleKotlinArchive").dependencyNames()
        assertEquals(
            setOf(
                "compileCommonMainKotlinMetadata",
                "compileKotlinJs",
                "compileKotlinMacosArm64",
                "compileKotlinWasmJs",
                "compileWebMainKotlinMetadata",
                "generateProjectStructureMetadata",
                "metadataCommonMainClasses",
                "metadataWebMainClasses"
            ).prettyPrinted,
            assembleTaskDependencies.prettyPrinted,
        )
    }

    @Test
    fun `assemble task depends on cinterops commonization and resources`() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                enableCInteropCommonization()
                enableMppResourcesPublication()
            }
        ) {
            kotlin {
                macosArm64 {
                    withCInterop("lib")
                    publishResources()
                }
                iosArm64 { withCInterop("lib") }
                linuxArm64 { withCInterop("lib") }
                js { publishResources() }
                wasmJs { publishResources() }
                jvm()
                publishing {
                    publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE)
                }
            }
        }
        project.evaluate()

        val assembleTaskDependencies = project.tasks.getByName("assembleKotlinArchive").dependencyNames()

        assertEquals(
            setOf(
                "cinteropLibIosArm64",
                "cinteropLibLinuxArm64",
                "cinteropLibMacosArm64",
                "commonizeCInterop",
                "compileAppleMainKotlinMetadata",
                "compileCommonMainKotlinMetadata",
                "compileKotlinIosArm64",
                "compileKotlinJs",
                "compileKotlinLinuxArm64",
                "compileKotlinMacosArm64",
                "compileKotlinWasmJs",
                "compileNativeMainKotlinMetadata",
                "compileWebMainKotlinMetadata",
                "generateProjectStructureMetadata",
                "jsCopyHierarchicalMultiplatformResources",
                "macosArm64CopyHierarchicalMultiplatformResources",
                "metadataAppleMainClasses",
                "metadataCommonMainClasses",
                "metadataNativeMainClasses",
                "metadataWebMainClasses",
                "wasmJsCopyHierarchicalMultiplatformResources",
            ).prettyPrinted,
            assembleTaskDependencies.prettyPrinted,
        )
    }

    @Test
    fun `project dependencies do not depend on KAR for compile tasks`() {
        val producer = buildProjectWithMPP(
            projectBuilder = { withName("producer") },
        ) {
            kotlin {
                js()
                publishing {
                    publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE)
                }
            }
        }
        val consumer = buildProjectWithMPP(
            projectBuilder = { withName("consumer").withParent(producer) },
        ) {
            kotlin {
                js()
                sourceSets.commonMain.dependencies {
                    implementation(project(":"))
                }
                publishing {
                    publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE)
                }
            }
        }
        producer.evaluate()

        consumer.populateTaskGraph(consumer.tasks.getByName("compileKotlinJs"))

        assertEquals(
            setOf(
                ":checkKotlinGradlePluginConfigurationErrors",
                ":compileKotlinJs",
                ":consumer:checkKotlinGradlePluginConfigurationErrors",
                ":consumer:compileKotlinJs",
                ":consumer:kmpPartiallyResolvedDependenciesChecker",
                ":kmpPartiallyResolvedDependenciesChecker",
            ).prettyPrinted,
            consumer.gradle.taskGraph.allTasks.map { it.path }.toSet().prettyPrinted,
        )
    }

    @Test
    fun `packs Kotlin Archive when no targets produce KAR content`() {
        val project = buildProjectWithMPP(
            preApplyCode = { setAndroidSdkDirProperty(project) },
        ) {
            plugins.apply("com.android.kotlin.multiplatform.library")
            kotlin {
                jvm()
                targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach { target ->
                    target.compileSdk = 34
                    target.namespace = "org.jetbrains.kotlin.testSample"
                }
                publishing {
                    publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE)
                }
            }
        }.evaluate()

        val assembleTask = project.tasks.getByName("assembleKotlinArchive") as AssembleKotlinArchiveTask
        val packTask = project.tasks.getByName("packKotlinArchive") as PackKotlinArchiveTask

        assembleTask.execute()
        packTask.execute()

        assertEquals(
            listOf(
                "cinterop/",
                "manifest.json",
                "metadata/",
                "platform/",
                "resources/",
            ).prettyPrinted,
            packTask.outputFile.get().asFile.zipXzArchiveEntries().sorted().prettyPrinted,
        )
    }

    @Test
    fun `packs all Kotlin Archive content according to the KAR layout`() {
        val project = buildProject()
        val assembleTask = project.tasks.register("testAssembleKotlinArchive", AssembleKotlinArchiveTask::class.java).get().apply {
            addPlatformKlib("js", project.singleFileTree("platform/js", "platform-klib"))
            addPlatformKlib("wasm", project.singleFileTree("platform/wasm", "platform-klib"))
            addPlatformKlib("macosArm64", project.singleFileTree("platform/macosArm64", "platform-klib"))
            addCInterop(project.provider { "macosArm64/cinteropName" }, project.singleFileTree("cinterop/macosArm64", "cinterop-klib"))
            addCInterop(project.provider { "iosArm64/cinteropName" }, project.singleFileTree("cinterop/iosArm64", "cinterop-klib"))
            addMetadataKlib("commonMain", project.singleFileTree("metadata/commonMain", "metadata-klib"))
            addMetadataKlib("nativeMain", project.singleFileTree("metadata/nativeMain", "metadata-klib"))
            addMetadataKlib("nativeMain-cinterop", project.singleFileTree("metadata/nativeMain-cinterop", "commonized-cinterop-klib"))
            addResources("js", project.singleFileTree("resources/js", "resources.txt"))
            addResources("macosArm64", project.singleFileTree("resources/macosArm64", "resources.txt"))

            projectStructureMetadataFile.set(project.textFile("project-structure-metadata.json"))
            outputDirectory.set(project.layout.buildDirectory.dir("kotlin-archive-test/assemble"))
        }
        val task = project.tasks.register("testPackKotlinArchive", PackKotlinArchiveTask::class.java).get().apply {
            assembledKarDirectory.set(assembleTask.outputDirectory)
            outputFile.set(project.layout.buildDirectory.file("kotlin-archive-test/test.kar.xz"))
        }
        assembleTask.execute()
        task.execute()

        assertEquals(
            listOf(
                "cinterop/",
                "cinterop/iosArm64/",
                "cinterop/iosArm64/cinteropName/",
                "cinterop/iosArm64/cinteropName/cinterop-klib",
                "cinterop/macosArm64/",
                "cinterop/macosArm64/cinteropName/",
                "cinterop/macosArm64/cinteropName/cinterop-klib",
                "manifest.json",
                "metadata/",
                "metadata/commonMain/",
                "metadata/commonMain/metadata-klib",
                "metadata/kotlin-project-structure-metadata.json",
                "metadata/nativeMain-cinterop/",
                "metadata/nativeMain-cinterop/commonized-cinterop-klib",
                "metadata/nativeMain/",
                "metadata/nativeMain/metadata-klib",
                "platform/",
                "platform/js/",
                "platform/js/platform-klib",
                "platform/macosArm64/",
                "platform/macosArm64/platform-klib",
                "platform/wasm/",
                "platform/wasm/platform-klib",
                "resources/",
                "resources/js/",
                "resources/js/resources.txt",
                "resources/macosArm64/",
                "resources/macosArm64/resources.txt",
            ).prettyPrinted,
            task.outputFile.get().asFile.zipXzArchiveEntries().sorted().prettyPrinted,
        )
    }

    private fun KotlinNativeTarget.withCInterop(name: String): KotlinNativeTarget = apply {
        compilations.getByName("main").cinterops.create(name)
    }

    private fun KotlinTarget.publishResources() {
        project.multiplatformExtension.resourcesPublicationExtension?.publishResourcesAsKotlinComponent(
            this,
            resourcePathForSourceSet = { sourceSet ->
                KotlinTargetResourcesPublication.ResourceRoot(
                    project.provider { File(sourceSet.name) },
                    emptyList(),
                    emptyList(),
                )
            },
            relativeResourcePlacement = project.provider { File("resources") },
        )
    }

    private fun Task.dependencyNames(): Set<String> = taskDependencies.getDependencies(this).mapTo(mutableSetOf()) { it.name }

    private fun Project.singleFileTree(directoryName: String, fileName: String): FileTree {
        val directory = layout.buildDirectory.dir("kotlin-archive-test/input/$directoryName").get().asFile
        directory.resolve(fileName).apply {
            parentFile.mkdirs()
            writeText("Content of $fileName")
        }
        return fileTree(directory)
    }

    private fun Project.textFile(fileName: String): File =
        layout.buildDirectory.file("kotlin-archive-test/input/$fileName").get().asFile.apply {
            parentFile.mkdirs()
            writeText("Project structure metadata")
        }

    private fun File.zipXzArchiveEntries(): List<String> =
        inputStream().buffered().use { fileInput ->
            XZCompressorInputStream(fileInput).use { xzInput ->
                ZipInputStream(xzInput).use { zipInput ->
                    generateSequence(zipInput::getNextEntry).map { entry -> entry.name }.toList()
                }
            }
        }
}

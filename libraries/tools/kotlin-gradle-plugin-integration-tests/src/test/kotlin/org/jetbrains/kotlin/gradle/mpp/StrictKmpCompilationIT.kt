/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.listProperty
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformCompilationTask
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.computeTransformedLibraryChecksum
import org.jetbrains.kotlin.gradle.uklibs.enableStrictKmpCompilation
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@MppGradlePluginTests
class StrictKmpCompilationIT : KGPBaseTest() {

    @GradleTestVersions(
        // Something explodes in old Gradle version with provider return
        minVersion = TestVersions.Gradle.MAX_SUPPORTED
    )
    @GradleTest
    fun `strict kmp compilation - coroutines and platform native libraries transform - in a project with 2 native targets`(version: GradleVersion) {
        val transformMap = projectWith2NativeTargets(version) {
            buildScriptInjection {
                project.computeTransformedLibraryChecksum(false)
                project.enableStrictKmpCompilation()
            }
        }.dumpSeparateCompilationXfragmentTransformBySourceSet {
            (project.extensions.getByName("kotlin") as KotlinMultiplatformExtension).iosArm64().compilations
                .get("main")
                .compileTaskProvider.get()
        }

        val commonMainTransform = assertNotNull(transformMap["commonMain"])
        val nativeMainTransform = assertNotNull(transformMap["nativeMain"])
        val appleMainTransform = assertNotNull(transformMap["appleMain"])
        assertEquals(
            listOf(
                "stdlib",
                "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-concurrentMain-.klib",
                "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-commonMain-.klib",
                "org.jetbrains.kotlinx-atomicfu-0.23.1-commonMain-.klib",
            ).prettyPrinted,
            commonMainTransform.map { it.name }.prettyPrinted
        )
        assertEquals(
            listOf(
                "org.jetbrains.kotlin.native.platform.iconv",
                "org.jetbrains.kotlin.native.platform.posix",
                "org.jetbrains.kotlin.native.platform.zlib",
                "org.jetbrains.kotlin.native.platform.builtin",
                "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-nativeDarwinMain-.klib",
                "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-nativeMain-.klib",
                "org.jetbrains.kotlinx-atomicfu-0.23.1-nativeMain-.klib",
                // FIXME: Take 2 components
                "org.jetbrains.kotlinx_atomicfu-cinterop-interop-.klib",
            ).prettyPrinted,
            nativeMainTransform.map { it.name }.prettyPrinted
        )

        val coreFoundationCommonization = assertNotNull(appleMainTransform.filter { it.name == "org.jetbrains.kotlin.native.platform.CoreFoundation" }.singleOrNull())
        assertEquals(
            "commonizer_native_targets=ios_arm64 ios_simulator_arm64 ios_x64 macos_arm64 tvos_arm64 tvos_simulator_arm64 watchos_arm64 watchos_device_arm64 watchos_simulator_arm64",
            coreFoundationCommonization.resolve("default/manifest").useLines {
                it.filter { it.startsWith("commonizer_native_targets=") }.single()
            }
        )

        // FIXME: Check that transform matches the -Xfragment output
    }

    @GradleTestVersions(
        minVersion = TestVersions.Gradle.MAX_SUPPORTED
    )
    @GradleTest
    fun `before strict kmp compilation - coroutines and platform native libraries transform - in a project with 2 native targets`(version: GradleVersion) {
        val project = projectWith2NativeTargets(version) {
            buildScriptInjection {
                project.computeTransformedLibraryChecksum(false)
                project.enableStrictKmpCompilation(false)
            }
        }

        val commonMainClasspath = project.dumpMetadataCompilationClasspath {
            (extensions.getByName("kotlin") as KotlinMultiplatformExtension).metadata().compilations.getByName("commonMain")
        }
        val coreFoundationCommonization = assertNotNull(commonMainClasspath.filter { it.name == "org.jetbrains.kotlin.native.platform.CoreFoundation" }.singleOrNull())
        assertEquals(
            "commonizer_native_targets=ios_arm64 ios_simulator_arm64",
            coreFoundationCommonization.resolve("default/manifest").useLines {
                it.filter { it.startsWith("commonizer_native_targets=") }.single()
            }
        )

        val coroutinesNativeMainVisibility = setOf(
            "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-nativeDarwinMain-.klib",
            "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-nativeMain-.klib",
            "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-concurrentMain-.klib",
            "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-commonMain-.klib",
            "org.jetbrains.kotlinx-atomicfu-0.23.1-nativeMain-.klib",
            "org.jetbrains.kotlinx-atomicfu-0.23.1-commonMain-.klib",
        )
        assertEquals(
            coroutinesNativeMainVisibility,
            commonMainClasspath.map { it.name }.intersect(coroutinesNativeMainVisibility),
        )
    }

    private fun TestProject.dumpSeparateCompilationXfragmentTransformBySourceSet(
        compileTask: Project.() -> K2MultiplatformCompilationTask
    ): Map<String, List<File>> {
        val taskName = buildScriptReturn {
            project.compileTask().name
        }.buildAndReturn()
        return providerBuildScriptReturn {
            project.provider {
                // delay evaluation till after fragment.dependencies is computed (after transforms)
                val outputProp = project.objects.listProperty<Pair<String, List<File>>>()
                project.compileTask().multiplatformStructure.fragments.get().map { fragment ->
                    fragment.dependencies.elements.map { fragment.fragmentName to it.map { it.asFile } }
                }.forEach {
                    outputProp.add(it)
                }
                outputProp
            }.flatMap { it }
        }.buildAndReturn(taskName).toMap()
    }

    private fun TestProject.dumpMetadataCompilationClasspath(
        compilation: Project.() -> KotlinMetadataCompilation<*>
    ): List<File> {
        val taskName = buildScriptReturn {
            project.compilation().compileTaskProvider.get().name
        }.buildAndReturn()
        return providerBuildScriptReturn {
            project.provider {
                // delay evaluation till after fragment.dependencies is computed (after transforms)
                project.compilation().compileDependencyFiles.elements.map { it.map { it.asFile } }
            }.flatMap { it }
        }.buildAndReturn(taskName)
    }

    private fun projectWith2NativeTargets(
        version: GradleVersion,
        setup: TestProject.() -> Unit = {
            buildScriptInjection {
                project.computeTransformedLibraryChecksum(false)
                project.enableStrictKmpCompilation()
            }
        },
    ) = project("empty", version) {
        setup()
        plugins {
            kotlin("multiplatform")
        }
        buildScriptInjection {
            project.applyMultiplatform {
                iosArm64()
                iosSimulatorArm64()

                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
                }
            }
        }
    }
}

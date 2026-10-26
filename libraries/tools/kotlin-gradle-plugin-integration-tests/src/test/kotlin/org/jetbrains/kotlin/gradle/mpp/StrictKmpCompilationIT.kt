/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.Project
import org.gradle.internal.impldep.com.google.api.client.util.Data.mapOf
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
import org.jetbrains.kotlin.gradle.uklibs.enableSeparateKmpCompilation
import org.jetbrains.kotlin.gradle.uklibs.enableStrictKmpCompilation
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@GradleTestVersions(
    // Something explodes in old Gradle version with provider return
    minVersion = TestVersions.Gradle.MAX_SUPPORTED
)
@MppGradlePluginTests
class StrictKmpCompilationIT : KGPBaseTest() {

    @GradleTest
    fun `strict kmp compilation - coroutines and platform native libraries transform - in a project with 2 native targets`(version: GradleVersion) {
        val transformMap = projectWith2NativeTargets(
            version,
            enableStrictKmpCompilation = true,
        ).dumpSeparateCompilationXfragmentTransformBySourceSet {
            (project.extensions.getByName("kotlin") as KotlinMultiplatformExtension).iosArm64().compilations
                .get("main")
                .compileTaskProvider.get()
        }

        val commonMainTransform = assertNotNull(transformMap["commonMain"])
        val nativeMainTransform = assertNotNull(transformMap["nativeMain"])
        val appleMainTransform = assertNotNull(transformMap["appleMain"])
        assertEquals(
            setOf(
                "stdlib",
                "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-concurrentMain-.klib",
                "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-commonMain-.klib",
                "org.jetbrains.kotlinx-atomicfu-0.23.1-commonMain-.klib",
            ).prettyPrinted,
            commonMainTransform.map { it.name }.toSet().prettyPrinted
        )
        assertEquals(
            setOf(
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
            nativeMainTransform.map { it.name }.toSet().prettyPrinted
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

    @GradleTest
    fun `before strict kmp compilation - coroutines and platform native libraries transform - in a project with 2 native targets`(version: GradleVersion) {
        // FIXME: Rewrite this with just separate compilation?
        val project = projectWith2NativeTargets(
            version,
            enableStrictKmpCompilation = false,
        )

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

    @GradleTest
    fun `strict kmp compilation - coroutines and guava - in a project with single jvm`(version: GradleVersion) {
        val strictCompilationTransformMap = projectWithSingleJvmTarget(
            version,
            enableStrictKmpCompilation = true,
        ).dumpSeparateCompilationXfragmentTransformBySourceSet {
            (project.extensions.getByName("kotlin") as KotlinMultiplatformExtension).jvm().compilations
                .get("main")
                .compileTaskProvider.get() as K2MultiplatformCompilationTask
        }

        val beforeStrictCompilationTransformMap = projectWithSingleJvmTarget(
            version,
            enableStrictKmpCompilation = false
        ).dumpSeparateCompilationXfragmentTransformBySourceSet {
            (project.extensions.getByName("kotlin") as KotlinMultiplatformExtension).jvm().compilations
                .get("main")
                .compileTaskProvider.get() as K2MultiplatformCompilationTask
        }

        assertEquals(
            mapOf(
                "commonMain" to setOf(
                    "guava-33.7.2-jre.jar",
                    "annotations-23.0.0.jar",
                    "failureaccess-1.0.3.jar",
                    "listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar",
                    "jspecify-1.0.1.jar",
                    "error_prone_annotations-2.50.0.jar",
                    "j2objc-annotations-3.1.jar",
                    "org.jetbrains.kotlin-kotlin-stdlib-2.5.255-SNAPSHOT-commonMain-.klib",
                    "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-concurrentMain-.klib",
                    "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-commonMain-.klib",
                    "kotlinx-coroutines-core-jvm-1.11.0.jar",
                ),
                "intermediateMain" to setOf(
                    "guava-33.7.2-jre.jar",
                    "annotations-23.0.0.jar",
                    "failureaccess-1.0.3.jar",
                    "listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar",
                    "jspecify-1.0.1.jar",
                    "error_prone_annotations-2.50.0.jar",
                    "j2objc-annotations-3.1.jar",
                    "kotlinx-coroutines-core-jvm-1.11.0.jar",
                ),
                "jvmMain" to setOf(
                ),
            ).prettyPrinted,
            beforeStrictCompilationTransformMap.mapValues { it.value.map { it.name }.toSet() }.prettyPrinted,
        )

        assertEquals(
            mapOf(
                "commonMain" to setOf(
                    "org.jetbrains.kotlin-kotlin-stdlib-2.5.255-SNAPSHOT-commonMain-.klib",
                    "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-concurrentMain-.klib",
                    "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-commonMain-.klib",
                ),
                "intermediateMain" to setOf(
                    "guava-33.7.2-jre.jar",
                    "annotations-23.0.0.jar",
                    "failureaccess-1.0.3.jar",
                    "listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar",
                    "jspecify-1.0.1.jar",
                    "error_prone_annotations-2.50.0.jar",
                    "j2objc-annotations-3.1.jar",
                    "kotlinx-coroutines-core-jvm-1.11.0.jar",
                ),
                "jvmMain" to setOf(
                ),
            ).prettyPrinted,
            strictCompilationTransformMap.mapValues { it.value.map { it.name }.toSet() }.prettyPrinted,
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
        enableStrictKmpCompilation: Boolean,
    ) = project("empty", version) {
        buildScriptInjection {
            project.computeTransformedLibraryChecksum(false)
            if (enableStrictKmpCompilation) {
                project.enableStrictKmpCompilation(true)
            } else {
                project.enableStrictKmpCompilation(false)
            }
        }
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

    private fun projectWithSingleJvmTarget(
        version: GradleVersion,
        enableStrictKmpCompilation: Boolean = true,
    ) = project("empty", version) {
        buildScriptInjection {
            project.computeTransformedLibraryChecksum(false)
            if (enableStrictKmpCompilation) {
                project.enableStrictKmpCompilation()
            } else {
                project.enableSeparateKmpCompilation()
            }
        }
        plugins {
            kotlin("multiplatform")
        }
        buildScriptInjection {
            project.applyMultiplatform {
                jvm()

                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0")
                    implementation("com.google.guava:guava:33.7.2-jre")
                }

                val intermediateMain = sourceSets.create("intermediateMain") {
                    it.dependsOn(sourceSets.commonMain.get())
                }

                sourceSets.jvmMain.get().dependsOn(intermediateMain)
            }
        }
    }
}

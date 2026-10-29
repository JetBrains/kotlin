/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.listProperty
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformCompilationTask
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.computeTransformedLibraryChecksum
import org.jetbrains.kotlin.gradle.uklibs.enableSeparateKmpCompilation
import org.jetbrains.kotlin.gradle.uklibs.enableStrictKmpCompilation
import org.jetbrains.kotlin.gradle.uklibs.publish
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
            commonMainTransform.map { it.name }.toSet().prettyPrinted,
            "We should only see commonMain and a custom source set. nativeMain or platform libraries commonization which would be previously visible shouldn't be seen"
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
            nativeMainTransform.map { it.name }.toSet().prettyPrinted,
            "In nativeMain we can see nativeMain and commonization results for all native targets"
        )

        val coreFoundationCommonization = assertNotNull(appleMainTransform.filter { it.name == "org.jetbrains.kotlin.native.platform.CoreFoundation" }.singleOrNull())
        assertEquals(
            "commonizer_native_targets=ios_arm64 ios_simulator_arm64 ios_x64 macos_arm64 tvos_arm64 tvos_simulator_arm64 watchos_arm64 watchos_device_arm64 watchos_simulator_arm64",
            coreFoundationCommonization.resolve("default/manifest").useLines {
                it.filter { it.startsWith("commonizer_native_targets=") }.single()
            },
            "Apple platform libraries commonization in appleMain intersects all Apple targets"
        )
    }

    @GradleTest
    fun `before strict kmp compilation - coroutines and platform native libraries transform - in a project with 2 native targets`(version: GradleVersion) {
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
            },
            "Before strict compilation we should see permissive intersection of Apple targets in commonMain",
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
            "All native/darwin coroutines fragments should be visible in commonMain",
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
                    "org.jetbrains.kotlin-kotlin-stdlib-2.5.255-SNAPSHOT-commonMain-.klib",
                    "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-concurrentMain-.klib",
                    "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-commonMain-.klib",
                    "kotlinx-coroutines-core-jvm-1.11.0.jar",
                ),
                "intermediateMain" to setOf(
                    "annotations-23.0.0.jar",
                    "guava-33.7.2-jre.jar",
                    "kotlinx-coroutines-core-jvm-1.11.0.jar",
                ),
                "jvmMain" to setOf(
                ),
            ).prettyPrinted,
            beforeStrictCompilationTransformMap.mapValues { it.value.map { it.name }.toSet() }.prettyPrinted,
            ""
        )

        assertEquals(
            mapOf(
                "commonMain" to setOf(
                    "org.jetbrains.kotlin-kotlin-stdlib-2.5.255-SNAPSHOT-commonMain-.klib",
                    "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-concurrentMain-.klib",
                    "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-commonMain-.klib",
                ),
                "intermediateMain" to setOf(
                    "annotations-23.0.0.jar",
                    "guava-33.7.2-jre.jar",
                    "kotlinx-coroutines-core-jvm-1.11.0.jar",
                ),
                "jvmMain" to setOf(
                ),
            ).prettyPrinted,
            strictCompilationTransformMap.mapValues { it.value.map { it.name }.toSet() }.prettyPrinted,
        )
    }

    @GradleTest
    fun `strict kmp compilation - custom source sets`(version: GradleVersion) {
        val producer = projectWithNonDefaultSourceSets(
            version,
            enableStrictKmpCompilation = true,
        ).publish(
            publisherConfiguration = PublisherConfiguration(
                group = "producer",
            )
        )

        val consumer = projectWithNonDefaultSourceSets(
            version,
            enableStrictKmpCompilation = true,
        )
        consumer.addPublishedProjectToRepositories(producer)
        consumer.buildScriptInjection {
            project.applyMultiplatform {
                sourceSets.commonMain.dependencies {
                    implementation(producer.rootCoordinate)
                }
            }
        }

        val strictCompilationTransformMap = consumer.dumpSeparateCompilationXfragmentTransformBySourceSet {
            (project.extensions.getByName("kotlin") as KotlinMultiplatformExtension).iosArm64().compilations
                .get("main")
                .compileTaskProvider.get() as K2MultiplatformCompilationTask
        }

        val strictCommonMainTransform = assertNotNull(strictCompilationTransformMap["commonMain"])
        val strictNativeMainTransform = assertNotNull(strictCompilationTransformMap["nativeMain"])
        val strictCustomMainTransform = assertNotNull(strictCompilationTransformMap["customMain"])
        val strictAppleMainTransform = assertNotNull(strictCompilationTransformMap["appleMain"])

        assertEquals(
            setOf(
                "org.jetbrains.kotlin-kotlin-stdlib-2.5.255-SNAPSHOT-commonMain-.klib",
                "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-commonMain-.klib",
                "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-concurrentMain-.klib",
                "producer-empty-1.0-commonMain-.klib",
            ).prettyPrinted,
            strictCommonMainTransform.map { it.name }.toSet().prettyPrinted,
            "Just sanity check commonMain; it's the same as non strict commonMain"
        )

        assertEquals(
            setOf(
                "org.jetbrains.kotlin.native.platform.builtin",
                "org.jetbrains.kotlin.native.platform.iconv",
                "org.jetbrains.kotlin.native.platform.posix",
                "org.jetbrains.kotlin.native.platform.zlib",
                "org.jetbrains.kotlinx-atomicfu-0.23.1-commonMain-.klib",
                "org.jetbrains.kotlinx-atomicfu-0.23.1-nativeMain-.klib",
                "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-nativeDarwinMain-.klib",
                "org.jetbrains.kotlinx-kotlinx-coroutines-core-1.11.0-nativeMain-.klib",
                "org.jetbrains.kotlinx_atomicfu-cinterop-interop-.klib",
                "producer-empty-1.0-customMain-.klib",
                "producer-empty-1.0-nativeMain-.klib",
                "stdlib",
            ).prettyPrinted,
            strictNativeMainTransform.map { it.name }.toSet().prettyPrinted,
            "nativeMain should be strict, that is to say we can see "
        )
        val nativeMainPosixCommonization = assertNotNull(strictNativeMainTransform.filter { it.name == "org.jetbrains.kotlin.native.platform.posix" }.singleOrNull())
        assertEquals(
            "commonizer_native_targets=ios_arm64 ios_simulator_arm64 ios_x64 linux_arm64 linux_x64 macos_arm64 mingw_x64 tvos_arm64 tvos_simulator_arm64 watchos_arm64 watchos_device_arm64 watchos_simulator_arm64",
            nativeMainPosixCommonization.resolve("default/manifest").useLines {
                it.filter { it.startsWith("commonizer_native_targets=") }.single()
            },
            "Expect strict commonization of nativeMain"
        )

        val customMainPosixCommonization = assertNotNull(strictCustomMainTransform.filter { it.name == "org.jetbrains.kotlin.native.platform.posix" }.singleOrNull())
        val appleMainPosixCommonization = assertNotNull(strictAppleMainTransform.filter { it.name == "org.jetbrains.kotlin.native.platform.posix" }.singleOrNull())
        assertEquals(
            "commonizer_native_targets=ios_arm64 ios_simulator_arm64",
            customMainPosixCommonization.resolve("default/manifest").useLines {
                it.filter { it.startsWith("commonizer_native_targets=") }.single()
            },
            "Source sets downstream of customMain should be non strict"
        )
        assertEquals(
            customMainPosixCommonization,
            appleMainPosixCommonization
        )

        assertEquals(
            setOf(
                "producer-empty-1.0-appleMain-.klib",
                "producer-empty-1.0-iosMain-.klib",
            ).prettyPrinted,
            strictCustomMainTransform.map { it.name }.filter { "producer" in it }.toSet().prettyPrinted,
            "Transform in customMain shouldn't be strict"
        )
        // appleMain and iosMain will already be non-strict in terms of transform because they see customMain that is non-strict?
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
                    implementation("com.google.guava:guava:33.7.2-jre") {
                        // Just test 1 Maven library, we don't care about transitive dependencies
                        isTransitive = false
                    }
                }

                val intermediateMain = sourceSets.create("intermediateMain") {
                    it.dependsOn(sourceSets.commonMain.get())
                }

                sourceSets.jvmMain.get().dependsOn(intermediateMain)
            }
        }
    }

    private fun projectWithNonDefaultSourceSets(
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
                iosArm64()
                iosSimulatorArm64()
                jvm()

                applyHierarchyTemplate {
                    common {
                        group("native") {
                            group("custom") {
                                group("apple") {
                                    group("macos") {
                                        withMacos()
                                    }
                                    group("ios") {
                                        withIos()
                                    }
                                }
                                group("mingw") {
                                    withMingw()
                                }
                            }
                            group("linux") {
                                withLinux()
                            }
                        }
                        group("jvm") {
                            withJvm()
                        }
                    }
                }

                sourceSets.configureEach {
                    it.compileSource("")
                }
                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
                }
            }
        }
    }
}

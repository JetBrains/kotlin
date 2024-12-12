/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createExternalKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.EmptyConsumingFragmentAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.findAllConsumableFor
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.validateKgpModelIsUklibCompliantAndCreateKgpFragments
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalWasmDsl::class)
class UklibFromKGPFragmentsGMTTests {

    @Test
    fun `uklib and GMT fragments - with multiple targets and a bamboo structure`() {
        buildProjectWithMPP {
            kotlin {
                iosArm64()
                iosX64()
                jvm()

                applyHierarchyTemplate {
                    group("customMain") {
                        group("customApple") {
                            group("customIos") {
                                withIosArm64()
                                withIosX64()
                            }
                        }
                        withJvm()
                    }
                }
            }

            runLifecycleAwareTest {
                assertEquals(
                    listOf(
                        TestFragment(
                            "customMainMain",
                            setOf("ios_arm64", "ios_x64", "jvm"),
                        ),
                    ),
                    multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments().map {
                        it.fragment
                    }.findAllConsumableFor(
                        attributes = setOf("ios_arm64", "ios_x64", "jvm"),
                    ).map { it.toTestFragment() },
                )

                // Compare sets with bamboos
                assertEquals(
                    listOf(
                        TestFragment(
                            "customAppleMain",
                            setOf("ios_arm64", "ios_x64"),
                        ),
                        TestFragment(
                            "customIosMain",
                            setOf("ios_arm64", "ios_x64"),
                        ),
                        TestFragment(
                            "customMainMain",
                            setOf("ios_arm64", "ios_x64", "jvm"),
                        ),
                    ).toSet(),
                    multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments().map {
                        it.fragment
                    }.findAllConsumableFor(
                        attributes = setOf("ios_arm64", "ios_x64"),
                    ).map { it.toTestFragment() }.toSet(),
                )

                KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()
                assertEquals(
                    setOf("ios_arm64", "ios_x64"),
                    project.locateOrRegisterMetadataDependencyTransformationTask(
                        multiplatformExtension.sourceSets.getByName("customIosMain")
                    ).get().transformationParameters.uklibFragmentAttributes
                )
                assertEquals(
                    setOf("ios_arm64", "ios_x64"),
                    project.locateOrRegisterMetadataDependencyTransformationTask(
                        multiplatformExtension.sourceSets.getByName("customAppleMain")
                    ).get().transformationParameters.uklibFragmentAttributes
                )
                assertEquals(
                    setOf("ios_arm64", "ios_x64", "jvm"),
                    project.locateOrRegisterMetadataDependencyTransformationTask(
                        multiplatformExtension.sourceSets.getByName("customMainMain")
                    ).get().transformationParameters.uklibFragmentAttributes
                )
            }
        }
    }

    @Test
    fun `uklib and GMT fragments - all supported targets - except external target`() {
        buildProjectWithMPP {
            androidLibrary { compileSdk = 31 }
            kotlin {
                iosArm64()
                iosX64()
                iosSimulatorArm64()
                linuxArm64()
                macosArm64()
                jvm()
                js()
                wasmJs()
                wasmWasi()
                androidTarget()
            }
        }.runLifecycleAwareTest {
            val fragments = multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments().map {
                it.fragment
            }
            val expectedCompilationClasspath = listOf(
                TestFragment(
                    identifier = "iosMain",
                    attributes = setOf(
                        "ios_arm64",
                        "ios_x64",
                        "ios_simulator_arm64",
                    )
                ),
                TestFragment(
                    identifier = "appleMain",
                    attributes = setOf(
                        "ios_arm64",
                        "ios_x64",
                        "ios_simulator_arm64",
                        "macos_arm64",
                    )
                ),
                TestFragment(
                    identifier = "nativeMain",
                    attributes = setOf(
                        "ios_arm64",
                        "ios_x64",
                        "ios_simulator_arm64",
                        "macos_arm64",
                        "linux_arm64",
                    )
                ),
                TestFragment(
                    identifier = "commonMain",
                    attributes = setOf(
                        "android",
                        "macos_arm64",
                        "ios_arm64",
                        "ios_x64",
                        "ios_simulator_arm64",
                        "linux_arm64",
                        "js_ir",
                        "jvm",
                        "wasm_js",
                        "wasm_wasi"
                    )
                ),
            )

            // Subset of consuming attributes
            assertEquals(
                expectedCompilationClasspath,
                fragments.findAllConsumableFor(
                    attributes = setOf("ios_arm64", "ios_x64"),
                ).map { it.toTestFragment() }
            )
            // Exactly matching consuming attributes
            assertEquals(
                expectedCompilationClasspath,
                fragments.findAllConsumableFor(
                    attributes = setOf("ios_arm64", "ios_x64", "ios_simulator_arm64"),
                ).map { it.toTestFragment() }
            )

            assertEquals(
                emptyList(),
                fragments.findAllConsumableFor(
                    attributes = setOf("ios_arm64", "ios_x64", "missing"),
                ).map { it.toTestFragment() }
            )

            assertEquals(
                setOf("ios_arm64", "ios_x64", "ios_simulator_arm64", "macos_arm64", "linux_arm64"),
                project.locateOrRegisterMetadataDependencyTransformationTask(
                    multiplatformExtension.sourceSets.getByName("nativeMain")
                ).get().transformationParameters.uklibFragmentAttributes
            )
            assertEquals(
                setOf("android", "macos_arm64", "linux_arm64", "ios_arm64", "ios_x64", "ios_simulator_arm64", "js_ir", "jvm", "wasm_js", "wasm_wasi"),
                project.locateOrRegisterMetadataDependencyTransformationTask(
                    multiplatformExtension.sourceSets.getByName("commonMain")
                ).get().transformationParameters.uklibFragmentAttributes
            )
        }
    }

    // Test external target separately because technically validateKgpModelIsUklibCompliantAndCreateKgpFragments is only for publication
    @Test
    fun `GMT attributes - external target - has some consuming attributes`() {
        val project = buildProjectWithMPP {
            kotlin {
                linuxX64()
                linuxArm64()
                createExternalKotlinTarget<FakeTarget> { defaults() }.createCompilation<FakeCompilation> { defaults(this@kotlin) }
            }
        }.evaluate()
        assertEquals(
            setOf(
                "external target", "linux_arm64", "linux_x64"
            ),
            // Makes sure we request some attributes for external target even if it will not resolve
            project.locateOrRegisterMetadataDependencyTransformationTask(
                project.multiplatformExtension.sourceSets.getByName("commonMain")
            ).get().transformationParameters.uklibFragmentAttributes
        )
    }

    //@Test
    fun `uklib and GMT fragments - diamonds`() {
        // FIXME: ...
    }

    @Test
    fun `form metadata compilation classpath without attributes`() {
        buildProjectWithMPP(
            preApplyCode = {
                publishUklib()
            }
        ) {
            runLifecycleAwareTest {
                assertEquals(
                    EmptyConsumingFragmentAttributes,
                    runCatching {
                        multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments().map {
                            it.fragment
                        }.findAllConsumableFor(attributes = emptySet())
                    }.exceptionOrNull()
                )
            }
        }
    }

    private data class TestFragment(
        val identifier: String,
        val attributes: Set<String>,
    )
    private fun UklibFragment.toTestFragment(): TestFragment = TestFragment(
        identifier = identifier,
        attributes = attributes,
    )
}
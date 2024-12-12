/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.formCompilationClasspathInConsumingModuleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.validateKgpModelIsUklibCompliantAndCreateKgpFragments
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.publishUklib
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UklibFromKGPFragmentsGMTTests {

    @Test
    fun `uklib and GMT fragments - with multiple targets and a bamboo structure`() {
        buildProjectWithMPP(
            preApplyCode = {
                publishUklib()
            }
        ) {
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
                            "iosArm64Main",
                            setOf("ios_arm64"),
                        ),
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
                    ),
                    multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments().map {
                        it.fragment
                    }.formCompilationClasspathInConsumingModuleFragment(
                        consumingFragmentAttributes = setOf("ios_arm64"),
                    ).map { it.toTestFragment() },
                )

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
                    ),
                    multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments().map {
                        it.fragment
                    }.formCompilationClasspathInConsumingModuleFragment(
                        consumingFragmentAttributes = setOf("ios_arm64", "ios_x64"),
                    ).map { it.toTestFragment() },
                )

                KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()
                assertEquals(
                    setOf("ios_arm64", "ios_x64"),
                    project.locateOrRegisterMetadataDependencyTransformationTask(
                        multiplatformExtension.sourceSets.getByName("customIosMain")
                    ).get().transformationParameters.uklibFragmentAttributes
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
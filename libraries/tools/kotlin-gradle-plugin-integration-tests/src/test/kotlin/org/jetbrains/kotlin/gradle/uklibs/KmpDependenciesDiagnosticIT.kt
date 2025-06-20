/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@OptIn(ExperimentalWasmDsl::class)
@MppGradlePluginTests
@DisplayName("Test diagnostics about consuming dependencies with inappropriate set of targets")
class KmpDependenciesDiagnosticIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() {
            val options = super.defaultBuildOptions
            return options.copy(
                nativeOptions = options.nativeOptions.copy(
                    enableKlibsCrossCompilation = true
                )
            )
        }

    @GradleTest
    fun `consumption with a subset of targets - metadata compilation with KotlinJsIrTarget transforms`(
        version: GradleVersion,
    ) {
        assertMetadataCompilationCanTransformDependencyWithSubsetOfTargets(
            producer = {
                linuxArm64()
                linuxX64()
            },
            consumer = {
                linuxArm64()
                linuxX64()
                js()
            },
            consumerBuildOptions = defaultBuildOptions.copy(
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
            ),
            gradleVersion = version,
        )
    }

    @GradleTest
    fun `consumption with a subset of targets - metadata compilation with Apple KotlinNativeTarget transforms`(
        version: GradleVersion,
    ) {
        assertMetadataCompilationCanTransformDependencyWithSubsetOfTargets(
            producer = {
                linuxArm64()
                linuxX64()
            },
            consumer = {
                linuxArm64()
                linuxX64()
                iosArm64()
                iosX64()
            },
            gradleVersion = version,
        )
    }

    private fun assertMetadataCompilationCanTransformDependencyWithSubsetOfTargets(
        producer: KotlinMultiplatformExtension.() -> Unit,
        consumer: KotlinMultiplatformExtension.() -> Unit,
        consumerBuildOptions: BuildOptions = defaultBuildOptions,
        gradleVersion: GradleVersion,
    ) {
        val publisher = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    producer()
                    sourceSets.commonMain.get().compileSource("class NotCommon")
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "dependency"))

        project(
            "empty",
            gradleVersion,
            buildOptions = consumerBuildOptions,
        ) {
            plugins {
                kotlin("multiplatform")
            }
            addPublishedProjectToRepositories(publisher)
            buildScriptInjection {
                project.applyMultiplatform {
                    consumer()
                    sourceSets.commonMain.dependencies {
                        implementation(publisher.rootCoordinate)
                    }
                    sourceSets.commonMain.get().compileSource("fun consume(notCommon: NotCommon) {}")
                }
            }

            // FIXME: KT-78494
            build("compileCommonMainKotlinMetadata")
        }
    }
}

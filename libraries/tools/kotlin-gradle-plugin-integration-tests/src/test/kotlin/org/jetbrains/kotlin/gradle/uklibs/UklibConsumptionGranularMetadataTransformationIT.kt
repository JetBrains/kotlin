/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals

@OptIn(ExperimentalKotlinGradlePluginApi::class)
@MppGradlePluginTests
@DisplayName("Test the GMT runtime behavior")
class UklibConsumptionGranularMetadataTransformationIT : KGPBaseTest() {

    /**
     * FIXME: Make cross-compilation a requirement of resolution with [KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs]
     * or test resolution without cross-compilation
     */

    @GradleTest
    fun `lenient PSM consumption in GMT`(
        version: GradleVersion
    ) {
        val subsetProducer = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosX64()
                    linuxArm64()
                    linuxX64()

                    sourceSets.all {
                        it.addIdentifierClass()
                    }
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "producer"))

        val consumer = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(subsetProducer)
            buildScriptInjection {
                project.computeTransformedLibraryChecksum(false)
                project.setUklibResolutionStrategy()
                project.enableCrossCompilation()
                project.applyMultiplatform {
                    iosArm64()
                    iosX64()
                    macosArm64()
                    macosX64()
                    linuxArm64()
                    linuxX64()

                    sourceSets.all {
                        it.addIdentifierClass()
                    }
                    sourceSets.commonMain.get().dependencies {
                        implementation(subsetProducer.rootCoordinate)
                    }
                }
            }
        }

        val expectedTransformations = mapOf<String, List<List<String>>>(
            // producer is missing macOS targets for these source sets
            "macosMain" to mutableListOf(),
            "appleMain" to mutableListOf(),
            "commonMain" to mutableListOf(),
            "iosMain" to mutableListOf(
                mutableListOf(
                    "iosMain",
                    "producer-empty-1.0-iosMain-.klib",
                ),
                mutableListOf(
                    "iosMain",
                    "producer-empty-1.0-appleMain-.klib",
                ),
                mutableListOf(
                    "iosMain",
                    "producer-empty-1.0-nativeMain-.klib",
                ),
                mutableListOf(
                    "iosMain",
                    "producer-empty-1.0-commonMain-.klib",
                ),
            ),
            "linuxMain" to mutableListOf(
                mutableListOf(
                    "linuxMain",
                    "producer-empty-1.0-linuxMain-.klib",
                ),
                mutableListOf(
                    "linuxMain",
                    "producer-empty-1.0-nativeMain-.klib",
                ),
                mutableListOf(
                    "linuxMain",
                    "producer-empty-1.0-commonMain-.klib",
                ),
            ),
        )
        assertEquals(
            expectedTransformations.prettyPrinted,
            expectedTransformations.mapValues {
                consumer.metadataTransformationOutputClasspath(
                    it.key
                ).relativeTransformationPathComponents()
            }.prettyPrinted
        )

        consumer.build("assemble")
    }
}
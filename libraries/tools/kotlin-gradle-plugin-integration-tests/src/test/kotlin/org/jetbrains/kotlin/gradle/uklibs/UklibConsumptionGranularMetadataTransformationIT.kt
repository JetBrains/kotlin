/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.pathString
import kotlin.test.assertEquals

@OptIn(ExperimentalKotlinGradlePluginApi::class)
@MppGradlePluginTests
@DisplayName("Test the GMT runtime behavior")
class UklibConsumptionGranularMetadataTransformationIT : KGPBaseTest() {

    @GradleTest
    fun `uklib consumption in GMT - parent transformations are reused in downstream compilations and fragments are interleaved`(
        version: GradleVersion
    ) {
        val transitivePublisher = publishUklib(
            version,
            PublisherConfiguration(name = "transitive"),
        ) {
            iosArm64()
            iosX64()
            macosArm64()
            macosX64()
            linuxArm64()
            linuxX64()

            applyHierarchyTemplate {
                group("transitiveProducerCommon") {
                    group("transitiveProducerApple") {
                        group("transitiveProducerIos") {
                            withIosArm64()
                            withIosX64()
                        }
                        group("transitiveProducerMacos") {
                            withMacosArm64()
                            withMacosX64()
                        }
                    }
                    group("transitiveProducerLinux") {
                        withLinuxArm64()
                        withLinuxX64()
                    }
                }
            }
        }

        val directPublisher = publishUklib(
            version,
            PublisherConfiguration(name = "direct"),
            transitivePublisher,
        ) {
            iosArm64()
            iosX64()
            macosArm64()
            macosX64()
            linuxArm64()
            linuxX64()

            applyHierarchyTemplate {
                group("directProducerCommon") {
                    group("directProducerApple") {
                        group("directProducerIos") {
                            withIosArm64()
                            withIosX64()
                        }
                        withMacosArm64()
                        withMacosX64()
                    }
                    group("directProducerLinux") {
                        withLinuxArm64()
                        withLinuxX64()
                    }
                }
            }

            sourceSets.getByName("directProducerCommonMain").dependencies {
                api(transitivePublisher.coordinate)
            }
        }

        project("buildScriptInjectionGroovy", version) {
            addPublishedProjectToRepositoriesAndIgnoreGradleMetadata(directPublisher)
            addPublishedProjectToRepositoriesAndIgnoreGradleMetadata(transitivePublisher)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.computeUklibChecksum(false)
                project.applyMultiplatform {
                    iosArm64()
                    iosX64()
                    macosArm64()
                    linuxArm64()

                    applyHierarchyTemplate {
                        group("consumerCommon") {
                            group("consumerApple") {
                                group("consumerIos") {
                                    withIosArm64()
                                    withIosX64()
                                }
                                withMacosArm64()
                                withMacosX64()
                            }
                            group("consumerLinux") {
                                withLinuxArm64()
                                withLinuxX64()
                            }
                        }
                    }

                    sourceSets.all {
                        it.addIdentifierClass()
                    }
                    sourceSets.getByName("consumerCommonMain").dependencies {
                        implementation(directPublisher.coordinate)
                    }
                }
            }

            assertEquals(
                listOf(
                    listOf("consumerIosMain", "uklib-foo-direct-1.0-directProducerIosMain-"),
                    listOf("consumerIosMain", "uklib-foo-transitive-1.0-transitiveProducerIosMain-"),
                    listOf("consumerAppleMain", "uklib-foo-direct-1.0-directProducerAppleMain-"),
                    listOf("consumerAppleMain", "uklib-foo-transitive-1.0-transitiveProducerAppleMain-"),
                    listOf("consumerCommonMain", "uklib-foo-direct-1.0-directProducerCommonMain-"),
                    listOf("consumerCommonMain", "uklib-foo-transitive-1.0-transitiveProducerCommonMain-"),
                ),
                metadataTransformationOutputClasspath("consumerIosMain").relativeTransformationPathComponents(),
            )
        }
    }

    /**
     * Consuming metadata compilations must see the fragments ordered by their attributes compatibility. In GMT this order depends on the
     * order of MetadataDependencyResolution.ChooseVisibleSourceSets.visibleSourceSetNamesExcludingDependsOn
     *
     * In this test we construct a uklib with many intermediate fragments and check that the output of the transformation is properly
     * ordered
     */
    @GradleTest
    fun `uklib consumption in GMT - output classpath of GMT is ordered according to the compatibility of Uklib fragments`(
        version: GradleVersion
    ) {
        val publishedProject = project("buildScriptInjectionGroovy", version) {
            buildScriptInjection {
                project.enableUklibPublication()
                project.applyMultiplatform {
                    applyHierarchyTemplate {
                        group("one") {
                            withIosArm64()
                            group("two") {
                                withIosX64()
                                group("three") {
                                    withLinuxArm64()
                                    group("four") {
                                        withLinuxX64()
                                        group("five") {
                                            withMacosArm64()
                                            group("six") {
                                                withMacosX64()
                                                group("seven") {
                                                    withJvm()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    iosArm64()
                    iosX64()
                    linuxArm64()
                    linuxX64()
                    macosArm64()
                    macosX64()
                    jvm()

                    sourceSets.all {
                        it.addIdentifierClass()
                    }
                }
            }
        }.publish()

        project("buildScriptInjectionGroovy", version) {
            addPublishedProjectToRepositoriesAndIgnoreGradleMetadata(publishedProject)
            buildScriptInjection {
                project.computeUklibChecksum(false)
                project.enableCrossCompilation()
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    jvm()
                    macosX64()

                    sourceSets.commonMain.get().addIdentifierClass()
                    sourceSets.commonMain.get().dependencies {
                        implementation(publishedProject.coordinate)
                    }
                }
            }

            assertEquals(
                listOf(
                    listOf("commonMain", "uklib-foo-producer-1.0-sixMain-"),
                    listOf("commonMain", "uklib-foo-producer-1.0-fiveMain-"),
                    listOf("commonMain", "uklib-foo-producer-1.0-fourMain-"),
                    listOf("commonMain", "uklib-foo-producer-1.0-threeMain-"),
                    listOf("commonMain", "uklib-foo-producer-1.0-twoMain-"),
                    listOf("commonMain", "uklib-foo-producer-1.0-oneMain-"),
                ),
                metadataTransformationOutputClasspath("commonMain")
                    .filterNot { "kotlin-stdlib" in it.name }
                    .relativeTransformationPathComponents(),
            )
        }
    }

    // Take full paths of the classpath formed by the GMT and extract last 2 path components for assertions
    private fun List<File>.relativeTransformationPathComponents(): List<List<String>> = map { it.lastPathComponents(2) }
    private fun File.lastPathComponents(number: Int): List<String> = toPath().toList().takeLast(number).map { it.pathString }

    private fun TestProject.metadataTransformationOutputClasspath(
        sourceSetName: String,
    ): List<File> {
        val iosMainTransformationTask = buildScriptReturn {
            project.locateOrRegisterMetadataDependencyTransformationTask(
                kotlinMultiplatform.sourceSets.getByName(sourceSetName)
            ).name
        }.buildAndReturn()
        val outputClasspath = buildScriptReturn {
            val transformationTask = project.locateOrRegisterMetadataDependencyTransformationTask(
                kotlinMultiplatform.sourceSets.getByName(sourceSetName)
            ).get()
            transformationTask.allTransformedLibraries().get()
        }.buildAndReturn(iosMainTransformationTask)
        return outputClasspath
    }

    private fun publishUklib(
        gradleVersion: GradleVersion,
        publisherConfiguration: PublisherConfiguration,
        addPublishedRepository: PublishedProject? = null,
        multiplatformConfiguration: KotlinMultiplatformExtension.() -> Unit,
    ): PublishedProject {
        return project("buildScriptInjectionGroovy", gradleVersion) {
            addPublishedRepository?.let { addPublishedProjectToRepositories(it) }
            buildScriptInjection {
                project.enableUklibPublication()
                project.applyMultiplatform {
                    multiplatformConfiguration()
                    sourceSets.all {
                        it.addIdentifierClass()
                    }
                }
            }
        }.publish(publisherConfiguration)
    }

}
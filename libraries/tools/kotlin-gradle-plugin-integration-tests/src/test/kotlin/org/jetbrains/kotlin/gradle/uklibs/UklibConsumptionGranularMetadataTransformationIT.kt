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
        val targetSubset: KotlinMultiplatformExtension.() -> Unit = {
            iosArm64()
            iosX64()
            linuxArm64()
            linuxX64()
        }
        val subsetProducer = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    targetSubset()
                    sourceSets.all {
                        it.compileStubSourceWithSourceSetName()
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
                project.applyMultiplatform {
                    targetSubset()
                    macosArm64()
                    macosX64()

                    sourceSets.all {
                        it.compileStubSourceWithSourceSetName()
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

    @GradleTest
    fun `uklib consumption in GMT - parent transformations are reused in downstream compilations and fragments are interleaved`(
        version: GradleVersion
    ) {
        val transitivePublisher = publishUklib(
            version,
            PublisherConfiguration(group = "transitive"),
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
            PublisherConfiguration(group = "direct"),
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
                api(transitivePublisher.rootCoordinate)
            }
        }

        project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(directPublisher)
            addPublishedProjectToRepositories(transitivePublisher)
            buildScriptInjection {
                project.computeTransformedLibraryChecksum(false)
                project.setUklibResolutionStrategy()
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
                        it.compileStubSourceWithSourceSetName()
                    }
                    sourceSets.getByName("consumerCommonMain").dependencies {
                        implementation(directPublisher.rootCoordinate)
                    }
                }
            }

            assertEquals(
                listOf(
                    listOf("consumerIosMain", "uklib-direct-empty-1.0-directProducerIosMain-"),
                    listOf("consumerIosMain", "uklib-transitive-empty-1.0-transitiveProducerIosMain-"),
                    listOf("consumerAppleMain", "uklib-direct-empty-1.0-directProducerAppleMain-"),
                    listOf("consumerAppleMain", "uklib-transitive-empty-1.0-transitiveProducerAppleMain-"),
                    listOf("consumerCommonMain", "uklib-direct-empty-1.0-directProducerCommonMain-"),
                    listOf("consumerCommonMain", "uklib-transitive-empty-1.0-transitiveProducerCommonMain-"),
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
        val publishedProject = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.setUklibPublicationStrategy()
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
                        it.compileStubSourceWithSourceSetName()
                    }
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "producer"))

        project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(publishedProject)
            buildScriptInjection {
                project.computeTransformedLibraryChecksum(false)
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    jvm()
                    macosX64()

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    sourceSets.commonMain.get().dependencies {
                        implementation(publishedProject.rootCoordinate)
                    }
                }
            }

            assertEquals(
                listOf(
                    listOf("commonMain", "uklib-producer-empty-1.0-sixMain-"),
                    listOf("commonMain", "uklib-producer-empty-1.0-fiveMain-"),
                    listOf("commonMain", "uklib-producer-empty-1.0-fourMain-"),
                    listOf("commonMain", "uklib-producer-empty-1.0-threeMain-"),
                    listOf("commonMain", "uklib-producer-empty-1.0-twoMain-"),
                    listOf("commonMain", "uklib-producer-empty-1.0-oneMain-"),
                ),
                metadataTransformationOutputClasspath("commonMain")
                    .filterNot { "kotlin-stdlib" in it.name }
                    .relativeTransformationPathComponents(),
            )
        }
    }

    @GradleTest
    fun `uklib consumption in GMT - with a subset of targets - in a source set with a superset of targets`(
        version: GradleVersion,
    ) {
        val publisher = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    sourceSets.commonMain.get().compileSource(
                        "class ProducerCommon"
                    )
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "dependency"))

        val consumingSourceSetProperty = "consumingSourceSet"
        project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(publisher)
            buildScriptInjection {
                project.computeTransformedLibraryChecksum(false)
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    jvm()
                    sourceSets.commonMain.dependencies {
                        implementation(publisher.rootCoordinate)
                    }
                    val consumer = project.providers.gradleProperty(consumingSourceSetProperty).orNull
                    sourceSets.configureEach {
                        if (it.name == consumer) {
                            it.compileSource(
                                "fun consume(param: ProducerCommon) {}"
                            )
                        }
                    }
                }
            }

            assertEquals(
                listOf<List<String>>(
                ).prettyPrinted,
                metadataTransformationOutputClasspath("commonMain")
                    .filterNot { "kotlin-stdlib" in it.name }
                    .relativeTransformationPathComponents().prettyPrinted,
            )
            assertEquals(
                listOf<List<String>>(
                    mutableListOf("nativeMain", "uklib-dependency-empty-1.0-commonMain-"),
                ).prettyPrinted,
                metadataTransformationOutputClasspath("linuxMain")
                    .filterNot { "kotlin-stdlib" in it.name }
                    .relativeTransformationPathComponents().prettyPrinted,
            )

            buildAndFail("compileCommonMainKotlinMetadata", "-P${consumingSourceSetProperty}=commonMain")
            build("compileLinuxMainKotlinMetadata", "-P${consumingSourceSetProperty}=linuxMain")

            val linuxMainMetadata = buildScriptReturn {
                kotlinMultiplatform.metadata().compilations.getByName("linuxMain").output.classesDirs.singleFile
            }.buildAndReturn()

            assertEquals(
                """
                /consume|consume(ProducerCommon){}[0]

                """.trimIndent(),
                dumpKlibMetadataSignatures(linuxMainMetadata),
            )
        }
    }

    private fun publishUklib(
        gradleVersion: GradleVersion,
        publisherConfiguration: PublisherConfiguration,
        addPublishedRepository: PublishedProject? = null,
        multiplatformConfiguration: KotlinMultiplatformExtension.() -> Unit,
    ): PublishedProject {
        return project("empty", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedRepository?.let { addPublishedProjectToRepositories(it) }
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    multiplatformConfiguration()
                    sourceSets.all {
                        it.compileStubSourceWithSourceSetName()
                    }
                }
            }
        }.publish(publisherConfiguration = publisherConfiguration)
    }

}
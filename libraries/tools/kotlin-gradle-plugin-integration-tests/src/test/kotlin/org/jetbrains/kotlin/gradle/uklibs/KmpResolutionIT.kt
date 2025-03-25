/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals

@MppGradlePluginTests
@DisplayName("Test the GMT runtime behavior")
class KmpResolutionIT : KGPBaseTest() {

    /**
     * FIXME: Make cross-compilation a requirement of resolution with [KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs]
     * or test resolution without cross-compilation
     */

    @GradleTest
    fun `smoke lenient kmp resolution - GMT and platform compilation - direct and transitive are kmp publications`(
        version: GradleVersion,
    ) {
        val consumer = transitiveConsumptionCase(
            version,
            transitiveConfiguration = {
                buildScriptInjection {
                    project.applyMultiplatform {
                        sourceSets.commonMain.get().addIdentifierClass()
                        sourceSets.linuxMain.get().addIdentifierClass()
                    }
                }
            },
            directConfiguration = {
                buildScriptInjection {
                    project.applyMultiplatform {
                        sourceSets.commonMain.get().addIdentifierClass()
                        sourceSets.linuxMain.get().addIdentifierClass()
                    }
                }
            },
            consumerConfiguration = {
                buildScriptInjection {
                    project.setUklibResolutionStrategy()
                }
            }
        )

        assertEquals(
            listOf<List<String>>(
                listOf("commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${consumer.buildOptions.kotlinVersion}-commonMain-.klib"),
                listOf("commonMain", "foo-transitive-1.0-commonMain-.klib"),
            ).prettyPrinted, consumer.metadataTransformationOutputClasspath("commonMain")
                .relativeTransformationPathComponents().prettyPrinted
        )
        assertEquals(
            listOf<List<String>>(
                listOf("linuxMain", "foo-direct-1.0-linuxMain-.klib"),
                listOf("linuxMain", "foo-direct-1.0-commonMain-.klib"),
                listOf("linuxMain", "foo-transitive-1.0-linuxMain-.klib"),
                listOf("commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${consumer.buildOptions.kotlinVersion}-commonMain-.klib"),
                listOf("commonMain", "foo-transitive-1.0-commonMain-.klib"),
            ).prettyPrinted, consumer.metadataTransformationOutputClasspath("linuxMain")
                .relativeTransformationPathComponents().prettyPrinted
        )

        val jvmDependencies = consumer.buildScriptReturn {
            project.ignoreAccessViolations {
                kotlinMultiplatform.jvm().compilationResolution()
            }
        }.buildAndReturn()
        val iosDependencies = consumer.buildScriptReturn {
            project.ignoreAccessViolations {
                kotlinMultiplatform.iosArm64().compilationResolution()
            }
        }.buildAndReturn()
        val linuxDependencies = consumer.buildScriptReturn {
            project.ignoreAccessViolations {
                kotlinMultiplatform.linuxArm64().compilationResolution()
            }
        }.buildAndReturn()

        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "metadataApiElements",
                ),
                "foo:transitive-jvm:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "not-a-metadata-jar",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "jvmApiElements-published",
                ),
                "foo:transitive:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "jvmApiElements-published",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:2.2.255-SNAPSHOT" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "not-a-metadata-jar",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "jvmApiElements",
                ),
                "org.jetbrains:annotations:13.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "not-a-metadata-jar",
                        ),
                    ),
                    configuration = "compile",
                ),
            ).prettyPrinted, jvmDependencies.prettyPrinted
        )
        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        // metadata jar is filtered
                    ),
                    configuration = "metadataApiElements",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:2.2.255-SNAPSHOT" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        // This is a stub in stdlib publication for native
                    ),
                    configuration = "nativeApiElements",
                ),
                "foo:transitive-iosarm64:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "org.jetbrains.kotlin.klib",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.usage" to "kotlin-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.native.target" to "ios_arm64",
                            "org.jetbrains.kotlin.platform.type" to "native",
                        ),
                    ),
                    configuration = "iosArm64ApiElements-published",
                ),
                "foo:transitive:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "iosArm64ApiElements-published",
                ),
            ).prettyPrinted, iosDependencies.prettyPrinted
        )
        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct-linuxarm64:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "org.jetbrains.kotlin.klib",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.usage" to "kotlin-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.native.target" to "linux_arm64",
                            "org.jetbrains.kotlin.platform.type" to "native",
                        ),
                    ),
                    configuration = "linuxArm64ApiElements-published",
                ),
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "linuxArm64ApiElements-published",
                ),
                "foo:transitive-linuxarm64:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "org.jetbrains.kotlin.klib",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.usage" to "kotlin-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.native.target" to "linux_arm64",
                            "org.jetbrains.kotlin.platform.type" to "native",
                        ),
                    ),
                    configuration = "linuxArm64ApiElements-published",
                ),
                "foo:transitive:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "linuxArm64ApiElements-published",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:2.2.255-SNAPSHOT" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "nativeApiElements",
                ),
            ).prettyPrinted, linuxDependencies.prettyPrinted
        )
    }


    private fun transitiveConsumptionCase(
        gradleVersion: GradleVersion,
        transitiveConfiguration: TestProject.() -> Unit,
        directConfiguration: TestProject.() -> Unit,
        consumerConfiguration: TestProject.() -> Unit,
    ): TestProject {
        val transitiveProducer = project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "transitive"
            }
            addKgpToBuildScriptCompilationClasspath()
            transitiveConfiguration()
            buildScriptInjection {
                project.enableCrossCompilation()
                project.applyMultiplatform {
                    js()
                    jvm()
                    iosArm64()
                    iosX64()
                    linuxArm64()
                    linuxX64()
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "foo"))

        val directProducer = project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "direct"
            }
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(transitiveProducer)
            directConfiguration()
            buildScriptInjection {
                project.enableCrossCompilation()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()

                    sourceSets.commonMain.get().dependencies {
                        api(transitiveProducer.rootCoordinate)
                    }
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "foo"))

        return project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "consumer"
            }
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(directProducer)
            addPublishedProjectToRepositories(transitiveProducer)
            consumerConfiguration()
            buildScriptInjection {
                project.computeTransformedLibraryChecksum(false)
                project.enableCrossCompilation()
                project.applyMultiplatform {
                    js()
                    jvm()
                    iosArm64()
                    iosX64()
                    linuxArm64()
                    linuxX64()

                    sourceSets.commonMain.get().dependencies {
                        implementation(directProducer.rootCoordinate)
                    }
                }
            }
        }
    }
}
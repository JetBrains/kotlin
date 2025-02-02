/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import org.gradle.api.internal.GradleInternal
import org.gradle.api.internal.project.ProjectStateRegistry
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.unitTests.uklibs.assertEqualsPP
import org.junit.jupiter.api.DisplayName
import org.jetbrains.kotlin.gradle.unitTests.uklibs.compilationRes
import org.jetbrains.kotlin.gradle.unitTests.uklibs.ResolvedComponentWithArtifacts
import kotlin.test.assertEquals

@OptIn(ExperimentalKotlinGradlePluginApi::class)
@MppGradlePluginTests
@DisplayName("Test the GMT runtime behavior")
class KmpResolutionIT : KGPBaseTest() {

    @GradleTest
    fun `smoke lenient resolution with GMT and platform compilation`(
        version: GradleVersion
    ) {
        val consumer = transitiveConsumptionCase(
            version,
            directConfiguration = {
                buildScriptInjection {
                    project.applyMultiplatform {
                        sourceSets.commonMain.get().addIdentifierClass()
                        sourceSets.linuxMain.get().addIdentifierClass()
                    }
                }
            }
        )

        assertEquals(
            listOf(
                listOf("commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${consumer.buildOptions.kotlinVersion}-commonMain-.klib"),
                listOf("commonMain", "transitive-empty-1.0-commonMain-.klib"),
            ),
            consumer.metadataTransformationOutputClasspath("commonMain")
                .relativeTransformationPathComponents(),
        )
        assertEquals(
            listOf(
                listOf("linuxMain", "direct-empty-1.0-linuxMain-.klib"),
                listOf("linuxMain", "direct-empty-1.0-commonMain-.klib"),
                listOf("linuxMain", "transitive-empty-1.0-linuxMain-.klib"),
                listOf("commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${consumer.buildOptions.kotlinVersion}-commonMain-.klib"),
                listOf("commonMain", "transitive-empty-1.0-commonMain-.klib"),
            ),
            consumer.metadataTransformationOutputClasspath("linuxMain")
                .relativeTransformationPathComponents(),
        )

        val jvmDependencies = consumer.buildScriptReturn {
            project.ignoreAccessViolations {
                kotlinMultiplatform.jvm().compilationRes()
            }
        }.buildAndReturn()
        val iosDependencies = consumer.buildScriptReturn {
            project.ignoreAccessViolations {
                kotlinMultiplatform.iosArm64().compilationRes()
            }
        }.buildAndReturn()
        val linuxDependencies = consumer.buildScriptReturn {
            project.ignoreAccessViolations {
                kotlinMultiplatform.linuxArm64().compilationRes()
            }
        }.buildAndReturn()

        assertEqualsPP(
            mutableMapOf(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        // This metadata jar got filtered out
                    ),
                    configuration = "metadataApiElements",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:2.2.255-SNAPSHOT" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.status" to "integration",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "non-a-metadata-jar",
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
                            "org.gradle.status" to "release",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "non-a-metadata-jar",
                        ),
                    ),
                    configuration = "compile",
                ),
                "transitive:empty-jvm:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.status" to "release",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "non-a-metadata-jar",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "jvmApiElements-published",
                ),
                "foo:transitive:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        // This is an available-at pointer
                    ),
                    configuration = "jvmApiElements-published",
                ),
            ),
            jvmDependencies,
        )
        assertEqualsPP(
            mutableMapOf(
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
                            "org.gradle.status" to "release",
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
            ),
            iosDependencies,
        )
        assertEqualsPP(
            mutableMapOf(
                "direct:empty-linuxarm64:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "org.jetbrains.kotlin.klib",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.status" to "release",
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
                "org.jetbrains.kotlin:kotlin-stdlib:2.2.255-SNAPSHOT" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "nativeApiElements",
                ),
                "foo:transitive-linuxarm64:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "org.jetbrains.kotlin.klib",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.status" to "release",
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
            ),
            linuxDependencies,
        )
    }

    @GradleTest
    fun `smoke lenient resolution with GMT and platform compilation - with uklib resolution and middle uklib producer`(
        version: GradleVersion
    ) {
        val consumer = transitiveConsumptionCase(
            version,
            transitiveConfiguration = {
                // transitive is regular KMP producer
            },
            directConfiguration = {
                // direct is Uklib producer
                buildScriptInjection {
                    project.enableUklibPublication()
                    project.applyMultiplatform {
                        sourceSets.commonMain.get().addIdentifierClass()
                    }
                }
            },
            consumerConfiguration = {
                // consumer can resolve Uklibs
                buildScriptInjection {
                    project.setUklibResolutionStrategy()
                }
            }
        )

        assertEqualsPP(
            mutableListOf(
                mutableListOf(
                    "commonMain", "org.jetbrains.kotlin-kotlin-stdlib-2.2.255-SNAPSHOT-commonMain-.klib",
                ),
                mutableListOf(
                    "commonMain", "foo-transitive-1.0-commonMain-.klib",
                ),
            ),
            consumer.metadataTransformationOutputClasspath("commonMain")
                .relativeTransformationPathComponents(),
        )
        assertEqualsPP(
            mutableListOf(
                mutableListOf(
                    "linuxMain", "uklib-foo-direct-1.0-commonMain-",
                ),
                mutableListOf(
                    "linuxMain", "foo-transitive-1.0-linuxMain-.klib",
                ),
                mutableListOf(
                    "commonMain", "org.jetbrains.kotlin-kotlin-stdlib-2.2.255-SNAPSHOT-commonMain-.klib",
                ),
                mutableListOf(
                    "commonMain", "foo-transitive-1.0-commonMain-.klib",
                ),
            ),
            consumer.metadataTransformationOutputClasspath("linuxMain")
                .relativeTransformationPathComponents(),
        )

        val jvmDependencies = consumer.buildScriptReturn {
            project.ignoreAccessViolations {
                kotlinMultiplatform.jvm().compilationRes()
            }
        }.buildAndReturn()
        val iosDependencies = consumer.buildScriptReturn {
            project.ignoreAccessViolations {
                kotlinMultiplatform.iosArm64().compilationRes()
            }
        }.buildAndReturn()
        val linuxDependencies = consumer.buildScriptReturn {
            project.ignoreAccessViolations {
                kotlinMultiplatform.linuxArm64().compilationRes()
            }
        }.buildAndReturn()

        // FIXME: DOM???
        assertEqualsPP(
            mutableMapOf(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.status" to "release",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "non-a-metadata-jar",
                        ),
                    ),
                    configuration = "uklib-jvmApiElements",
                ),
                "foo:transitive-jvm:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.status" to "release",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "non-a-metadata-jar",
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
                            "org.gradle.status" to "integration",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "non-a-metadata-jar",
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
                            "org.gradle.status" to "release",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "non-a-metadata-jar",
                        ),
                    ),
                    configuration = "compile",
                ),
            ),
            jvmDependencies,
        )
        assertEqualsPP(
            mutableMapOf(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.status" to "release",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "non-a-metadata-jar",
                        ),
                    ),
                    configuration = "uklib-jvmApiElements",
                ),
                "foo:transitive-iosarm64:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "org.jetbrains.kotlin.klib",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.status" to "release",
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
                "org.jetbrains.kotlin:kotlin-stdlib:2.2.255-SNAPSHOT" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "nativeApiElements",
                ),
            ),
            iosDependencies,
        )
        assertEqualsPP(
            mutableMapOf(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.status" to "release",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.isMetadataJar" to "non-a-metadata-jar",
                        ),
                    ),
                    configuration = "uklib-jvmApiElements",
                ),
                "foo:transitive-linuxarm64:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "org.jetbrains.kotlin.klib",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.status" to "release",
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
            ),
            linuxDependencies,
        )
    }

    private fun transitiveConsumptionCase(
        gradleVersion: GradleVersion,
        transitiveConfiguration: TestProject.() -> Unit = {},
        directConfiguration: TestProject.() -> Unit = {},
        consumerConfiguration: TestProject.() -> Unit = {},
    ): TestProject {
        val transitiveProducer = project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "transitive"
            }
            addKgpToBuildScriptCompilationClasspath()
            transitiveConfiguration()
            buildScriptInjection {
                project.applyMultiplatform {
                    js()
                    jvm()
                    iosArm64()
                    iosX64()
                    linuxArm64()
                    linuxX64()

                    sourceSets.commonMain.get().addIdentifierClass()
                    sourceSets.linuxMain.get().addIdentifierClass()
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
                project.setUklibResolutionStrategy()
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
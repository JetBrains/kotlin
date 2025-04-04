/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.*
import org.jetbrains.kotlin.gradle.testing.PrettyPrint
import org.jetbrains.kotlin.gradle.testing.ResolvedComponentWithArtifacts
import org.junit.jupiter.api.DisplayName
import kotlin.String
import kotlin.collections.Map
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
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                        sourceSets.linuxMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            },
            directConfiguration = {
                buildScriptInjection {
                    project.applyMultiplatform {
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                        sourceSets.linuxMain.get().compileStubSourceWithSourceSetName()
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
        assertEquals<PrettyPrint<List<List<String>>>>(
            mutableListOf<MutableList<String>>(
                mutableListOf("commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${consumer.buildOptions.kotlinVersion}-commonMain-.klib"),
                mutableListOf("commonMain", "foo-transitive-1.0-commonMain-.klib"),
            ).prettyPrinted, consumer.metadataTransformationOutputClasspath("iosMain")
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
                "org.jetbrains.kotlin:kotlin-stdlib:${consumer.buildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
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
                "org.jetbrains.kotlin:kotlin-stdlib:${consumer.buildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
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
                "org.jetbrains.kotlin:kotlin-stdlib:${consumer.buildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "nativeApiElements",
                ),
            ).prettyPrinted, linuxDependencies.prettyPrinted
        )
    }

    @GradleTest
    fun `smoke lenient kmp resolution - GMT and platform compilation - with direct uklib producer`(
        version: GradleVersion,
    ) {
        val consumer = transitiveConsumptionCase(
            version,
            transitiveConfiguration = {
                buildScriptInjection {
                    project.applyMultiplatform {
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                        sourceSets.linuxMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            },
            directConfiguration = {
                buildScriptInjection {
                    project.setUklibPublicationStrategy()
                    project.applyMultiplatform {
                        // Now it can't have linuxMain due to bamboos
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            },
            consumerConfiguration = {
                buildScriptInjection {
                    project.setUklibResolutionStrategy()
                }
            }
        )

        assertEquals<PrettyPrint<List<List<String>>>>(
            mutableListOf<MutableList<String>>(
                mutableListOf(
                    "commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${consumer.buildOptions.kotlinVersion}-commonMain-.klib",
                ),
                mutableListOf(
                    "commonMain", "foo-transitive-1.0-commonMain-.klib",
                ),
            ).prettyPrinted, consumer.metadataTransformationOutputClasspath("commonMain")
                .relativeTransformationPathComponents().prettyPrinted
        )
        assertEquals<PrettyPrint<List<List<String>>>>(
            mutableListOf<MutableList<String>>(
                mutableListOf(
                    "linuxMain", "uklib-foo-direct-1.0-commonMain-",
                ),
                mutableListOf(
                    "linuxMain", "foo-transitive-1.0-linuxMain-.klib",
                ),
                mutableListOf(
                    "commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${consumer.buildOptions.kotlinVersion}-commonMain-.klib",
                ),
                mutableListOf(
                    "commonMain", "foo-transitive-1.0-commonMain-.klib",
                ),
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
                        // Ignored because it doesn't have a jvm target
                    ),
                    configuration = "uklibApiElements",
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
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
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
                        // Ignored because it doesn't have an iOS target
                    ),
                    configuration = "uklibApiElements",
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
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "nativeApiElements",
                ),
            ).prettyPrinted, iosDependencies.prettyPrinted
        )
        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "uklib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "linux_arm64",
                        ),
                    ),
                    configuration = "uklibApiElements",
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
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "nativeApiElements",
                ),
            ).prettyPrinted, linuxDependencies.prettyPrinted
        )
    }

    @GradleTest
    fun `smoke lenient kmp resolution - GMT and platform compilation - with direct and transitive uklib producer`(
        version: GradleVersion,
    ) {
        val consumer = transitiveConsumptionCase(
            version,
            transitiveConfiguration = {
                // transitive is regular KMP producer
                buildScriptInjection {
                    project.setUklibPublicationStrategy()
                    project.applyMultiplatform {
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                        sourceSets.linuxMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            },
            directConfiguration = {
                // direct is Uklib producer
                buildScriptInjection {
                    project.setUklibPublicationStrategy()
                    project.setUklibResolutionStrategy()
                    project.applyMultiplatform {
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
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

        assertEquals<PrettyPrint<List<List<String>>>>(
            mutableListOf<MutableList<String>>(
                mutableListOf(
                    "commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${consumer.buildOptions.kotlinVersion}-commonMain-.klib",
                ),
                mutableListOf(
                    "commonMain", "uklib-foo-transitive-1.0-commonMain-",
                ),
            ).prettyPrinted, consumer.metadataTransformationOutputClasspath("commonMain")
                .relativeTransformationPathComponents().prettyPrinted
        )
        assertEquals<PrettyPrint<List<List<String>>>>(
            mutableListOf<MutableList<String>>(
                mutableListOf(
                    "linuxMain", "uklib-foo-direct-1.0-commonMain-",
                ),
                mutableListOf(
                    "linuxMain", "uklib-foo-transitive-1.0-linuxMain-",
                ),
                mutableListOf(
                    "commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${consumer.buildOptions.kotlinVersion}-commonMain-.klib",
                ),
                mutableListOf(
                    "commonMain", "uklib-foo-transitive-1.0-commonMain-",
                ),
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
                    configuration = "uklibApiElements",
                ),
                "foo:transitive:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "uklib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "jvm",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
                "org.jetbrains.kotlin:kotlin-dom-api-compat:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "commonFakeApiElements-published",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
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
                    ),
                    configuration = "uklibApiElements",
                ),
                "foo:transitive:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "uklib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "ios_arm64",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
                "org.jetbrains.kotlin:kotlin-dom-api-compat:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "commonFakeApiElements-published",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "nativeApiElements",
                ),
            ).prettyPrinted, iosDependencies.prettyPrinted
        )
        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "uklib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "linux_arm64",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
                "foo:transitive:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "uklib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "linux_arm64",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
                "org.jetbrains.kotlin:kotlin-dom-api-compat:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "commonFakeApiElements-published",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "nativeApiElements",
                ),
            ).prettyPrinted, linuxDependencies.prettyPrinted
        )
    }

    @GradleTest
    fun `smoke lenient kmp resolution - GMT and platform compilation - with transitive uklib producer through direct kmp dependency`(
        version: GradleVersion,
    ) {
        val consumer = transitiveConsumptionCase(
            version,
            transitiveConfiguration = {
                buildScriptInjection {
                    project.setUklibPublicationStrategy()
                    project.applyMultiplatform {
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                        sourceSets.linuxMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            },
            directConfiguration = {
                buildScriptInjection {
                    project.setUklibResolutionStrategy()
                    project.applyMultiplatform {
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                        sourceSets.linuxMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            },
            consumerConfiguration = {
                buildScriptInjection {
                    project.setUklibResolutionStrategy()
                }
            }
        )

        assertEquals<PrettyPrint<List<List<String>>>>(
            mutableListOf<MutableList<String>>(
                mutableListOf(
                    "commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${consumer.buildOptions.kotlinVersion}-commonMain-.klib",
                ),
                mutableListOf(
                    "commonMain", "uklib-foo-transitive-1.0-commonMain-",
                ),
            ).prettyPrinted, consumer.metadataTransformationOutputClasspath("commonMain")
                .relativeTransformationPathComponents().prettyPrinted
        )
        assertEquals<PrettyPrint<List<List<String>>>>(
            mutableListOf<MutableList<String>>(
                mutableListOf(
                    "linuxMain", "foo-direct-1.0-linuxMain-.klib",
                ),
                mutableListOf(
                    "linuxMain", "foo-direct-1.0-commonMain-.klib",
                ),
                mutableListOf(
                    "linuxMain", "uklib-foo-transitive-1.0-linuxMain-",
                ),
                mutableListOf(
                    "commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${consumer.buildOptions.kotlinVersion}-commonMain-.klib",
                ),
                mutableListOf(
                    "commonMain", "uklib-foo-transitive-1.0-commonMain-",
                ),
            ).prettyPrinted, consumer.metadataTransformationOutputClasspath("linuxMain")
                .relativeTransformationPathComponents().prettyPrinted
        )
        assertEquals<PrettyPrint<List<List<String>>>>(
            mutableListOf<MutableList<String>>(
                mutableListOf("commonMain", "org.jetbrains.kotlin-kotlin-stdlib-${consumer.buildOptions.kotlinVersion}-commonMain-.klib"),
                mutableListOf("commonMain", "uklib-foo-transitive-1.0-commonMain-"),
            ).prettyPrinted, consumer.metadataTransformationOutputClasspath("iosMain")
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
                "foo:transitive:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "uklib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "jvm",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
                "org.jetbrains.kotlin:kotlin-dom-api-compat:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "commonFakeApiElements-published",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
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
                    ),
                    configuration = "metadataApiElements",
                ),
                "foo:transitive:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "uklib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "ios_arm64",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
                "org.jetbrains.kotlin:kotlin-dom-api-compat:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "commonFakeApiElements-published",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "nativeApiElements",
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
                "foo:transitive:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "uklib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "linux_arm64",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
                "org.jetbrains.kotlin:kotlin-dom-api-compat:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "commonFakeApiElements-published",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "nativeApiElements",
                ),
            ).prettyPrinted, linuxDependencies.prettyPrinted
        )
    }

    /**
     * FIXME: Test Java base plugin resolution in detail because we use different resolvable configuration for Java compilations and
     * Kotlin jvm compilations
     */
    @GradleTest
    fun `smoke lenient java consumption - java resolvable configurations can fallback to metadata`(
        version: GradleVersion,
    ) {
        val producer = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "producer"))

        project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(producer)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    jvm()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    sourceSets.commonMain.dependencies {
                        api(producer.rootCoordinate)
                    }
                }
            }

            assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
                mutableMapOf<String, ResolvedComponentWithArtifacts>(
                    "org.jetbrains.kotlin:kotlin-stdlib:${buildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
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
                    "producer:empty:1.0" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                        ),
                        configuration = "metadataApiElements",
                    ),
                ).prettyPrinted,
                buildScriptReturn {
                    project.ignoreAccessViolations {
                        project.configurations.getByName(
                            java.sourceSets.getByName("jvmMain").compileClasspathConfigurationName
                        ).resolveProjectDependencyComponentsWithArtifacts()
                    }
                }.buildAndReturn("assemble").prettyPrinted
            )
        }
    }

    @GradleTest
    fun `java consumption - with direct lenient Uklib producer and a native-only transitive producer - results in a resolution failure in the consumer`(
        version: GradleVersion,
    ) {
        val transitive = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "transitive"))

        val direct = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(transitive)
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    jvm()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    sourceSets.commonMain.dependencies {
                        api(transitive.rootCoordinate)
                    }
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "direct"))

        project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(transitive)
            addPublishedProjectToRepositories(direct)
            buildScriptInjection {
                project.plugins.apply("java-library")
                project.configurations.named("implementation").configure {
                    it.dependencies.add(project.dependencies.create(direct.rootCoordinate))
                }
                java.sourceSets.getByName("main").compileJavaSource(
                    project,
                    className = "Consumer",
                    """
                        public class Consumer { }
                    """.trimIndent()
                )
            }

            // FIXME: Properly test which configuration failed to resolve
            buildAndFail("assemble") {
                assertOutputContains("No matching variant of transitive:empty:1.0 was found")
            }
        }
    }

    private fun transitiveConsumptionCase(
        gradleVersion: GradleVersion,
        transitiveConfiguration: TestProject.() -> Unit,
        directConfiguration: TestProject.() -> Unit,
        consumerConfiguration: TestProject.() -> Unit,
    ): TestProject {
        val consumedTargetConfiguration: KotlinMultiplatformExtension.() -> Unit = {
            js()
            jvm()
            iosArm64()
            iosX64()
            linuxArm64()
            linuxX64()
        }
        val intermediateSubsetTargetConfiguration: KotlinMultiplatformExtension.() -> Unit = {
            linuxArm64()
            linuxX64()
        }


        val transitiveProducer = project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "transitive"
            }
            addKgpToBuildScriptCompilationClasspath()
            transitiveConfiguration()
            buildScriptInjection {
                project.applyMultiplatform {
                    consumedTargetConfiguration()
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
                    intermediateSubsetTargetConfiguration()
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
                project.applyMultiplatform {
                    consumedTargetConfiguration()
                    sourceSets.commonMain.get().dependencies {
                        implementation(directProducer.rootCoordinate)
                    }
                }
            }
        }
    }
}
@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.FilePathRegex
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.dependsOnDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.projectArtifactDependency
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.iosMain
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.testing.*
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.tooling.core.closure
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UklibInterprojectResolutionTests {

    @Test
    fun `interproject uklib resolution - transitive dependency through jvm library - resolves uklib variants in transitive dependency`() {
        val targets: KotlinMultiplatformExtension.() -> Unit = {
            iosArm64()
            iosX64()
            jvm()
            js()
        }
        val root = buildProject()
        projectWithUklibs(root, "producer") {
            kotlin { targets() }
        }.evaluate()

        val directJavaConsumer = buildProject(
            projectBuilder = {
                withParent(root)
                withName("directJavaConsumer")
            }
        ) {
            plugins.apply("java-library")
            dependencies {
                "api"(project(":producer"))
            }
        }.evaluate()

        val transitiveUklibConsumer = projectWithUklibs(root, "transitiveUklibConsumer") {
            kotlin {
                targets()
                dependencies {
                    implementation(project(":directJavaConsumer"))
                }
            }
        }.evaluate()

        assertEquals(
            mapOf(
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "jvmApiElements",
                ),
            ).prettyPrinted,
            directJavaConsumer.configurations.getByName("compileClasspath")
                .resolveProjectDependencyComponentsWithArtifacts()
                .prettyPrinted
        )
        assertEquals(
            mapOf(
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-runtime",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "jvmRuntimeElements",
                ),
            ).prettyPrinted,
            directJavaConsumer.configurations.getByName("runtimeClasspath")
                .resolveProjectDependencyComponentsWithArtifacts()
                .prettyPrinted
        )

        assertResolvedCompilationArtifactMatchesAttributes(
            transitiveUklibConsumer.multiplatformExtension.iosArm64(),
            mutableMapOf(
                /** FIXME: KT-81055 java classes will not be filtered by [org.jetbrains.kotlin.gradle.tasks.canKlibBePassedToCompiler] */
                ":directJavaConsumer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.dependency.bundling" to "external",
                            "org.gradle.jvm.version" to "17",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                        ),
                    ),
                    configuration = "apiElements",
                ),
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "ios_arm64",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
            ),
        )

        assertResolvedCompilationArtifactMatchesAttributes(
            transitiveUklibConsumer.multiplatformExtension.jvm(),
            mapOf(
                ":directJavaConsumer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.dependency.bundling" to "external",
                            "org.gradle.jvm.version" to "17",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                        ),
                    ),
                    configuration = "apiElements",
                ),
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "jvm",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
            )
        )
        assertResolvedRuntimeArtifactMatchesAttributes(
            transitiveUklibConsumer.multiplatformExtension.jvm(),
            mapOf(
                ":directJavaConsumer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.dependency.bundling" to "external",
                            "org.gradle.jvm.version" to "17",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-runtime",
                        ),
                    ),
                    configuration = "runtimeElements",
                ),
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "kotlin-uklib-runtime",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "jvm",
                        ),
                    ),
                    configuration = "uklibRuntimeElements",
                ),
            )
        )

        listOf(
            transitiveUklibConsumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration,
            transitiveUklibConsumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration,
        ).forEach {
            assertEquals(
                mapOf(
                    ":directJavaConsumer" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "jar",
                                "org.gradle.category" to "library",
                                "org.gradle.dependency.bundling" to "external",
                                "org.gradle.jvm.version" to "17",
                                "org.gradle.libraryelements" to "jar",
                                "org.gradle.usage" to "java-api",
                            ),
                        ),
                        configuration = "apiElements",
                    ),
                    ":producer" to ResolvedComponentWithArtifacts(
                        configuration = "uklibApiElements",
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "uklibManifest",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "whole_uklib",
                            ),
                        ),
                    ),
                ).prettyPrinted,
                it.resolveProjectDependencyComponentsWithArtifacts().prettyPrinted,
                it.name,
            )
        }
    }


    @Test
    fun `interproject uklib resolution - direct dependency a uklib producing component - with matching set of targets`() {
        val targets: KotlinMultiplatformExtension.() -> Unit = {
            iosArm64()
            iosX64()
            jvm()
            js()
        }
        val root = buildProject()
        projectWithUklibs(root, "producer") {
            kotlin { targets() }
        }.evaluate()

        val consumer = projectWithUklibs(root, "consumer") {
            kotlin {
                targets()
                sourceSets.commonMain.dependencies {
                    implementation(project(":producer"))
                }
            }
        }.evaluate()

        assertResolvedCompilationArtifactMatchesAttributes(
            consumer.multiplatformExtension.iosArm64(),
            mapOf(
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "ios_arm64",
                        )
                    ),
                    configuration = "uklibApiElements",
                ),
            ),
        )

        assertResolvedCompilationArtifactMatchesAttributes(
            consumer.multiplatformExtension.jvm(),
            mapOf(
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "jvm",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
            )
        )
        assertResolvedRuntimeArtifactMatchesAttributes(
            consumer.multiplatformExtension.jvm(),
            mapOf(
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "kotlin-uklib-runtime",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "jvm",
                        ),
                    ),
                    configuration = "uklibRuntimeElements",
                ),
            )
        )

        assertResolvedCompilationArtifactMatchesAttributes(
            consumer.multiplatformExtension.js(),
            mapOf(
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "js_ir",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
            )
        )
        assertResolvedRuntimeArtifactMatchesAttributes(
            consumer.multiplatformExtension.js(),
            mapOf(
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-runtime",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "js_ir",
                        ),
                    ),
                    configuration = "uklibRuntimeElements",
                ),
            )
        )

        listOf(
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration,
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration,
        ).forEach {
            assertEquals(
                mapOf(
                    ":producer" to ResolvedComponentWithArtifacts(
                        configuration = "uklibApiElements",
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "uklibManifest",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "whole_uklib",
                            ),
                        ),
                    ),
                ).prettyPrinted,
                it.resolveProjectDependencyComponentsWithArtifacts().prettyPrinted,
                it.name,
            )
        }
    }

    @Test
    fun `interproject uklib resolution - direct dependency on a uklib producing component - with a subset of targets`() {
        val root = buildProject()
        projectWithUklibs(root, "producer") {
            kotlin {
                iosArm64()
            }
        }.evaluate()

        val consumer = projectWithUklibs(root, "consumer") {
            kotlin {
                iosArm64()
                iosSimulatorArm64()
                iosX64()
                jvm()
                js()
                sourceSets.commonMain.dependencies {
                    implementation(project(":producer"))
                }
            }
        }.evaluate()

        listOf(
            consumer.multiplatformExtension.iosSimulatorArm64().compilationResolution(),
            consumer.multiplatformExtension.jvm().compilationResolution(),
            consumer.multiplatformExtension.js().compilationResolution(),
        ).forEach {
            assertEquals(
                mapOf(
                    ":producer" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(),
                        configuration = "uklibApiElements",
                    ),
                ).prettyPrinted,
                it.prettyPrinted,
                it.toString(),
            )
        }
        listOf(
            consumer.multiplatformExtension.jvm().runtimeResolution(),
            consumer.multiplatformExtension.js().runtimeResolution(),
        ).forEach {
            assertEquals(
                mapOf(
                    ":producer" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(),
                        configuration = "uklibRuntimeElements",
                    ),
                ).prettyPrinted,
                it.prettyPrinted,
                it.toString(),
            )
        }

        listOf(
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration,
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration,
        ).forEach {
            assertEquals(
                mapOf(
                    ":producer" to ResolvedComponentWithArtifacts(
                        configuration = "uklibApiElements",
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "uklibManifest",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "whole_uklib",
                            ),
                        ),
                    ),
                ).prettyPrinted,
                it.resolveProjectDependencyComponentsWithArtifacts().prettyPrinted,
                it.name,
            )
        }
    }

    @Test
    fun `interproject uklib resolution - transitive dependency though a uklib producing component - with a subset of targets`() {
        val targets: KotlinMultiplatformExtension.() -> Unit = {
            iosArm64()
            iosX64()
            jvm()
            js()
        }
        val root = buildProject()
        projectWithUklibs(root, "producer") {
            kotlin { targets() }
        }.evaluate()

        projectWithUklibs(root, "intermediate") {
            kotlin {
                jvm()
                sourceSets.commonMain.dependencies {
                    api(project(":producer"))
                }
            }
        }.evaluate()

        val consumer = projectWithUklibs(root, "consumer") {
            kotlin {
                targets()
                sourceSets.commonMain.dependencies {
                    implementation(project(":intermediate"))
                }
            }
        }.evaluate()

        assertResolvedCompilationArtifactMatchesAttributes(
            consumer.multiplatformExtension.iosArm64(),
            mapOf(
                ":intermediate" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "uklibApiElements",
                ),
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "ios_arm64",
                        )
                    ),
                    configuration = "uklibApiElements",
                ),
            ),
        )

        assertResolvedCompilationArtifactMatchesAttributes(
            consumer.multiplatformExtension.jvm(),
            mapOf(
                ":intermediate" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "jvm",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "jvm",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
            )
        )
        assertResolvedRuntimeArtifactMatchesAttributes(
            consumer.multiplatformExtension.jvm(),
            mapOf(
                ":intermediate" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "kotlin-uklib-runtime",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "jvm",
                        ),
                    ),
                    configuration = "uklibRuntimeElements",
                ),
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "kotlin-uklib-runtime",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "jvm",
                        ),
                    ),
                    configuration = "uklibRuntimeElements",
                ),
            )
        )

        assertResolvedCompilationArtifactMatchesAttributes(
            consumer.multiplatformExtension.js(),
            mapOf(
                ":intermediate" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "uklibApiElements",
                ),
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "js_ir",
                        ),
                    ),
                    configuration = "uklibApiElements",
                ),
            )
        )
        assertResolvedRuntimeArtifactMatchesAttributes(
            consumer.multiplatformExtension.js(),
            mapOf(
                ":intermediate" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "uklibRuntimeElements",
                ),
                ":producer" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.usage" to "kotlin-uklib-runtime",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.uklib" to "true",
                            "org.jetbrains.kotlin.uklibState" to "decompressed",
                            "org.jetbrains.kotlin.uklibView" to "js_ir",
                        ),
                    ),
                    configuration = "uklibRuntimeElements",
                ),
            )
        )

        listOf(
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration,
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration,
        ).forEach {
            assertEquals(
                mapOf(
                    ":intermediate" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "uklibManifest",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "whole_uklib",
                            ),
                        ),
                        configuration = "uklibApiElements",
                    ),
                    ":producer" to ResolvedComponentWithArtifacts(
                        configuration = "uklibApiElements",
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "uklibManifest",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "whole_uklib",
                            ),
                        ),
                    ),
                ).prettyPrinted,
                it.resolveProjectDependencyComponentsWithArtifacts().prettyPrinted,
                it.name,
            )
        }
    }

    @Test
    fun `interproject uklib resolution - task dependencies`() = testInterprojectTaskDependenciesWithUklibs(
        enableCInteropCommonization = false,
        resolveIdeDependencies = mutableSetOf(
            "consumer:commonizeNativeDistribution",
            "producer:serializeUklibManifestWithoutCompilationDependency",
        ),
        compileCommonMainKotlinMetadata = mutableSetOf(
            "consumer:transformCommonMainDependenciesMetadata",
            "producer:commonizeNativeDistribution",
            "producer:compileCommonMainKotlinMetadata",
            "producer:compileLinuxMainKotlinMetadata",
            "producer:compileNativeMainKotlinMetadata",
            "producer:serializeUklibManifestWithoutCompilationDependency",
            "producer:transformCommonMainDependenciesMetadata",
            "producer:transformLinuxMainDependenciesMetadata",
            "producer:transformNativeMainDependenciesMetadata",
        ),
        compileKotlinJvm = mutableSetOf(
            "producer:compileJvmMainJava",
            "producer:compileKotlinJvm",
            "producer:jvmJar",
        ),
        compileKotlinLinuxX64 = mutableSetOf(
            "producer:compileKotlinLinuxX64",
        ),
        compileKotlinJs = mutableSetOf(
            "producer:compileKotlinJs",
            "producer:jsJar",
        ),
        transformLinuxMainCInteropDependenciesMetadata = null,
        runJvm = mutableSetOf(
            "consumer:compileJvmMainJava",
            "consumer:compileKotlinJvm",
            "producer:compileJvmMainJava",
            "producer:compileKotlinJvm",
            "producer:jvmJar",
        ),
    )

    @Test
    fun `interproject uklib resolution - task dependencies - with commonizer`() = testInterprojectTaskDependenciesWithUklibs(
        enableCInteropCommonization = true,
        resolveIdeDependencies = mutableSetOf(
            "consumer:cinteropConsumerLinuxArm64",
            "consumer:cinteropConsumerLinuxX64",
            "consumer:commonizeCInterop",
            "consumer:commonizeNativeDistribution",
            "consumer:copyCommonizeCInteropForIde",
            "consumer:linuxArm64Cinterop-consumerKlib",
            "consumer:linuxX64Cinterop-consumerKlib",
            "consumer:transformLinuxMainCInteropDependenciesMetadataForIde",
            "consumer:transformLinuxTestCInteropDependenciesMetadataForIde",
            "consumer:transformNativeMainCInteropDependenciesMetadataForIde",
            "consumer:transformNativeTestCInteropDependenciesMetadataForIde",
            // FIXME: KT-81139: Why is :producer:commonizeCInterop missing here?
            "producer:cinteropProducerLinuxArm64",
            "producer:cinteropProducerLinuxX64",
            "producer:serializeUklibManifestWithoutCompilationDependency",
        ),
        compileCommonMainKotlinMetadata = mutableSetOf(
            "consumer:transformCommonMainDependenciesMetadata",
            "producer:cinteropProducerLinuxArm64",
            "producer:cinteropProducerLinuxX64",
            "producer:commonizeCInterop",
            "producer:commonizeNativeDistribution",
            "producer:compileCommonMainKotlinMetadata",
            "producer:compileLinuxMainKotlinMetadata",
            "producer:compileNativeMainKotlinMetadata",
            "producer:serializeUklibManifestWithoutCompilationDependency",
            "producer:transformLinuxMainCInteropDependenciesMetadata",
            "producer:transformLinuxMainDependenciesMetadata",
            "producer:transformCommonMainDependenciesMetadata",
            "producer:transformNativeMainCInteropDependenciesMetadata",
            "producer:transformNativeMainDependenciesMetadata",
        ),
        compileKotlinJvm = mutableSetOf(
            "producer:compileJvmMainJava",
            "producer:compileKotlinJvm",
            "producer:jvmJar",
        ),
        compileKotlinLinuxX64 = mutableSetOf(
            "consumer:cinteropConsumerLinuxX64",
            "producer:cinteropProducerLinuxX64",
            "producer:compileKotlinLinuxX64",
        ),
        compileKotlinJs = mutableSetOf(
            "producer:compileKotlinJs",
            "producer:jsJar",
        ),
        transformLinuxMainCInteropDependenciesMetadata = mutableSetOf(
            "producer:serializeUklibManifestWithoutCompilationDependency",
        ),
        runJvm = mutableSetOf(
            "consumer:compileJvmMainJava",
            "consumer:compileKotlinJvm",
            "producer:compileJvmMainJava",
            "producer:compileKotlinJvm",
            "producer:jvmJar",
        ),
    )

    fun testInterprojectTaskDependenciesWithUklibs(
        enableCInteropCommonization: Boolean,
        resolveIdeDependencies: MutableSet<String>,
        transformLinuxMainCInteropDependenciesMetadata: MutableSet<String>?,
        compileCommonMainKotlinMetadata: MutableSet<String>,
        compileKotlinJvm: MutableSet<String>,
        compileKotlinLinuxX64: MutableSet<String>,
        compileKotlinJs: MutableSet<String>,
        runJvm: MutableSet<String>,
    ) {
        val targets: KotlinMultiplatformExtension.() -> Unit = {
            val nativeTargets = listOf(
                linuxArm64(),
                linuxX64(),
            )
            if (enableCInteropCommonization) {
                nativeTargets.forEach { target ->
                    target.compilations.getByName("main").cinterops.create(target.project.name) {
                        it.definitionFile.set(target.project.layout.projectDirectory.file("${target.project}.def"))
                    }
                }
            }
            jvm {
                binaries {
                    executable { }
                }
            }
            js()
        }
        val root = buildProject()
        val producer = projectWithUklibs(
            root,
            "producer",
            preApplyCode = {
                enableCInteropCommonization(enableCInteropCommonization)
            }
        ) {
            kotlin { targets() }
        }.evaluate()

        val consumer = projectWithUklibs(
            root,
            "consumer",
            preApplyCode = {
                enableCInteropCommonization(enableCInteropCommonization)
            }
        ) {
            kotlin {
                targets()
                sourceSets.commonMain.dependencies {
                    implementation(project(":producer"))
                }
            }
        }.evaluate()

        fun assertTaskDependenciesEqual(
            expectedTaskDependencies: Set<String>,
            consumerTaskName: String,
        ) = assertEquals(
            expectedTaskDependencies.prettyPrinted,
            consumer.tasks.getByName(consumerTaskName).closure {
                it.taskDependencies.getDependencies(null)
            }.filter { task ->
                return@filter !listOf(
                    // isLifecycleTask
                    { task.javaClass.superclass == DefaultTask::class.java },
                    // isIrrelevantProcessResourcesTask
                    { task is ProcessResources },
                    // checkers
                    { task.name in setOf("checkKotlinGradlePluginConfigurationErrors", "kmpPartiallyResolvedDependenciesChecker") }
                ).any { it() }
            }.map {
                "${it.project.name}:${it.name}"
            }.toSet().prettyPrinted,
            message = consumerTaskName,
        )

        assertTaskDependenciesEqual(
            resolveIdeDependencies,
            "resolveIdeDependencies"
        )

        assertTaskDependenciesEqual(
            compileCommonMainKotlinMetadata,
            "compileCommonMainKotlinMetadata"
        )

        val linuxCinteropTransform = "transformLinuxMainCInteropDependenciesMetadata"
        transformLinuxMainCInteropDependenciesMetadata?.let {
            assertTaskDependenciesEqual(
                transformLinuxMainCInteropDependenciesMetadata,
                linuxCinteropTransform
            )
        } ?: assert(linuxCinteropTransform !in consumer.tasks.names)

        assertTaskDependenciesEqual(
            compileKotlinJvm,
            "compileKotlinJvm"
        )
        assertTaskDependenciesEqual(
            runJvm,
            "runJvm"
        )

        assertTaskDependenciesEqual(
            compileKotlinLinuxX64,
            "compileKotlinLinuxX64"
        )

        assertTaskDependenciesEqual(
            compileKotlinJs,
            "compileKotlinJs"
        )
    }

    @Test
    fun `KT-77389 - java runtime classpath resolution - doesn't fail with resolution ambiguity`() {
        val root = buildProject { }
        buildProject(projectBuilder = {
            withParent(root)
            withName("producer")
        }) {
            plugins.apply("java-library")
        }.evaluate()

        val consumer = projectWithUklibs(root, "consumer") {
            kotlin {
                jvm()
                dependencies {
                    implementation(project(":producer"))
                }
            }
        }.evaluate()

        val files = consumer.configurations.getByName("jvmRuntimeClasspath").incoming.artifacts.map { it.file }
        val jar = assertNotNull(files.singleOrNull(), files.toString())
        assert(jar.path.endsWith("producer.jar")) { jar }
    }

    @Test
    fun `KT-77367 - IDE resolution - doesn't fail due to transforms for interproject dependencies in a clean state`() {
        val root = buildProject { }
        buildProjectWithJvm(
            projectBuilder = {
                withParent(root)
                withName("producer")
            },
            preApplyCode = {
                enableDefaultStdlibDependency(false)
            }
        ).evaluate()

        val consumer = projectWithUklibs(root, "consumer") {
            kotlin {
                jvm()
                dependencies {
                    implementation(project(":producer"))
                }
            }
        }.evaluate()

        val dependencies = consumer.kotlinIdeMultiplatformImport.resolveDependencies(
            consumer.multiplatformExtension.sourceSets.getByName("jvmMain")
        )
        dependencies.assertMatches(
            projectArtifactDependency(IdeaKotlinSourceDependency.Type.Regular, ":producer", FilePathRegex(".*/producer/build/libs/producer.jar")),
            dependsOnDependency(":consumer/commonMain"),
        )
    }

    private fun projectWithUklibs(
        root: Project,
        name: String,
        preApplyCode: Project.() -> Unit = {},
        code: Project.() -> Unit
    ) = buildProjectWithMPP(
        projectBuilder = {
            withName(name)
            withParent(root)
        },
        preApplyCode = {
            preApplyCode()
            setUklibPublicationStrategy()
            setUklibResolutionStrategy(KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs)
            enableDefaultStdlibDependency(false)
            enableDefaultJsDomApiDependency(false)
        },
        code = code,
    )

    fun assertResolvedCompilationArtifactMatchesAttributes(
        target: KotlinTarget,
        resolvedComponents: Map<ComponentPath, ResolvedComponentWithArtifacts>,
    ) {
        assertEquals(
            resolvedComponents.prettyPrinted,
            target.compilationResolution().prettyPrinted,
            target.name,
        )
    }

    fun assertResolvedRuntimeArtifactMatchesAttributes(
        target: KotlinTarget,
        resolvedComponents: Map<ComponentPath, ResolvedComponentWithArtifacts>,
    ) {
        assertEquals(
            resolvedComponents.prettyPrinted,
            target.runtimeResolution().prettyPrinted,
            target.name,
        )
    }

}
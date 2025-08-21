/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.internal.component.resolution.failure.exception.VariantSelectionByAttributesException
import org.gradle.internal.exceptions.MultiCauseException
import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.iosMain
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.unitTests.uklibs.GradleMetadataComponent.Variant
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.util.setUklibResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.testing.*
import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.util.assertDoesNotThrow
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalWasmDsl::class)
class UklibResolutionTestsWithMockComponents {
    @get:Rule
    val tmpDir = TemporaryFolder()

    @Test
    fun `uklib resolution - direct dependency on a pure uklib component`() {
        val repo = generateMockRepository(
            tmpDir,
            listOf(
                GradleComponent(
                    GradleMetadataComponent(
                        component = directGradleComponent,
                        variants = listOf(
                            uklibApiVariant,
                            jvmApiVariant,
                        ),
                    ),
                    directMavenComponent("uklib"),
                )
            )
        )

        val consumer = uklibConsumer {
            kotlin {
                iosArm64()
                iosX64()
                jvm()
                sourceSets.commonMain.dependencies { implementation("foo:direct:1.0") }
            }
            repositories.maven(repo)
        }

        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    configuration = uklibApiVariant.name,
                    artifacts = mutableListOf(uklibApiVariant.attributes + uklibTransformationIosArm64Attributes)
                ),
            ).prettyPrinted, consumer.multiplatformExtension.iosArm64().compilationResolution().prettyPrinted
        )
        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    configuration = uklibApiVariant.name,
                    artifacts = mutableListOf(uklibApiVariant.attributes + uklibTransformationJvmAttributes)
                ),
            ).prettyPrinted, consumer.multiplatformExtension.jvm().compilationResolution().prettyPrinted
        )
        listOf(
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts(),
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts(),
        ).forEach {
            assertEquals(
                mapOf<String, ResolvedComponentWithArtifacts>(
                    "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                        configuration = uklibApiVariant.name,
                        artifacts = mutableListOf(uklibApiVariant.attributes + uklibTransformationMetadataAttributes)
                    ),
                ).prettyPrinted,
                it.prettyPrinted,
            )
        }
    }

    @Test
    fun `uklib resolution - direct dependency on a KMP component`() {
        val repo = generateMockRepository(
            tmpDir,
            listOf(
                GradleComponent(
                    GradleMetadataComponent(
                        component = directGradleComponent,
                        variants = listOf(
                            kmpMetadataJarVariant,
                            kmpIosArm64MetadataJarVariant,
                            kmpIosArm64KlibVariant,
                            kmpJvmApiVariant,
                        ),
                    ),
                    directMavenComponent(),
                ),
            )
        )

        val consumer = uklibConsumer {
            kotlin {
                iosArm64()
                jvm()
                sourceSets.commonMain.dependencies { implementation("foo:direct:1.0") }
            }
            repositories.maven(repo)
        }

        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    configuration = kmpIosArm64KlibVariant.name,
                    artifacts = mutableListOf(
                        kmpIosArm64KlibVariant.attributes + klibCinteropCommonizerType,
                    )
                ),
            ).prettyPrinted, consumer.multiplatformExtension.iosArm64().compilationResolution().prettyPrinted
        )
        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    configuration = kmpJvmApiVariant.name,
                    artifacts = mutableListOf(
                        kmpJvmApiVariant.attributes + notAMetadataJar + jarArtifact,
                    )
                ),
            ).prettyPrinted, consumer.multiplatformExtension.jvm().compilationResolution().prettyPrinted
        )
        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    configuration = kmpMetadataJarVariant.name,
                    artifacts = mutableListOf(
                        kmpMetadataJarVariant.attributes + maybeAMetadataJar + jarArtifact + libraryElementsJar,
                    )
                ),
            ).prettyPrinted,
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts().prettyPrinted
        )
    }

    @Test
    fun `uklib resolution - fallback to metadata variant when platform variant is missing and filter out metadata jar`() {
        val repo = generateMockRepository(
            tmpDir,
            listOf(
                GradleComponent(
                    GradleMetadataComponent(
                        component = directGradleComponent,
                        variants = listOf(
                            kmpMetadataJarVariant,
                            kmpIosArm64MetadataJarVariant,
                            kmpIosArm64KlibVariant,
                        ),
                    ),
                    directMavenComponent(),
                ),
            )
        )

        val consumer = uklibConsumer {
            kotlin {
                iosArm64()
                iosX64()
                jvm()
                js()
                wasmJs()
                wasmWasi()
                sourceSets.commonMain.dependencies { implementation("foo:direct:1.0") }
            }
            repositories.maven(repo)
        }

        // Resolvable metadata configurations resolve as usual
        listOf(
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts(),
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts(),
        ).forEach {
            assertEquals(
                mapOf<String, ResolvedComponentWithArtifacts>(
                    "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            kmpMetadataVariantAttributes + maybeAMetadataJar + jarArtifact + libraryElementsJar
                        ),
                        configuration = "metadataApiElements",
                    ),
                ).prettyPrinted,
                it.prettyPrinted,
            )
        }

        // All the missing in producer targets resolve into metadata variant and filter out the metadata jar
        listOf(
            { consumer.multiplatformExtension.iosX64().compilationResolution() },
            { consumer.multiplatformExtension.jvm().compilationResolution() },
            { consumer.multiplatformExtension.js().compilationResolution() },
            { consumer.multiplatformExtension.wasmJs().compilationResolution() },
            { consumer.multiplatformExtension.wasmWasi().compilationResolution() },
        ).forEach {
            assertEquals(
                mapOf<String, ResolvedComponentWithArtifacts>(
                    "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                        configuration = kmpMetadataJarVariant.name,
                        artifacts = mutableListOf(
                            // This artifact is filtered out by a transform
                        )
                    ),
                ).prettyPrinted,
                it().prettyPrinted,
            )
        }
    }

    @Test
    fun `uklib resolution - fallback to metadata variant for transitive dependencies`() {
        val repo = generateMockRepository(
            tmpDir,
            listOf(
                GradleComponent(
                    GradleMetadataComponent(
                        component = directGradleComponent,
                        variants = listOf(
                            kmpMetadataJarVariant(
                                dependencies = listOf(
                                    transitiveGradleComponent.requiresDependency,
                                )
                            ),
                            kmpIosArm64MetadataJarVariant,
                            kmpIosArm64KlibVariant,
                            kmpJvmApiVariant,
                        ),
                    ),
                    directMavenComponent(),
                ),
                GradleComponent(
                    GradleMetadataComponent(
                        component = transitiveGradleComponent,
                        variants = listOf(
                            kmpMetadataJarVariant,
                            kmpIosArm64MetadataJarVariant,
                            kmpIosArm64KlibVariant,
                            kmpIosX64MetadataJarVariant,
                            kmpIosX64KlibVariant,
                            kmpJvmApiVariant,
                        ),
                    ),
                    transitiveMavenComponent()
                )
            )
        )

        val consumer = uklibConsumer {
            kotlin {
                iosArm64()
                iosX64()
                jvm()
                sourceSets.commonMain.dependencies { implementation("foo:direct:1.0") }
            }
            repositories.maven(repo)
        }

        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    configuration = kmpMetadataJarVariant.name,
                    artifacts = mutableListOf(
                        // This artifact is filtered out by a transform
                    )
                ),
                "foo:transitive:1.0" to ResolvedComponentWithArtifacts(
                    configuration = kmpIosX64KlibVariant.name,
                    artifacts = mutableListOf(
                        kmpIosX64KlibVariant.attributes + klibCinteropCommonizerType,
                    )
                ),
            ).prettyPrinted,
            consumer.multiplatformExtension.iosX64().compilationResolution().prettyPrinted
        )
    }

    @Test
    fun `uklib resolution - all configurations can resolve dom-api-compat`() {
        val consumer = uklibConsumer {
            repositories.mavenLocal()
            kotlin {
                iosArm64()
                iosX64()
                jvm()
                js()
                wasmJs()
                sourceSets.commonMain.dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-dom-api-compat:${project.kotlinToolingVersion}").also {
                        (it as ModuleDependency).isTransitive = false
                    }
                }
            }
        }

        listOf(
            consumer.multiplatformExtension.iosArm64().compilations.getByName("main").internal.configurations.compileDependencyConfiguration,
            consumer.multiplatformExtension.jvm().compilations.getByName("main").internal.configurations.compileDependencyConfiguration,
            consumer.multiplatformExtension.wasmJs().compilations.getByName("main").internal.configurations.compileDependencyConfiguration,
        ).forEach {
            assertEquals(
                mapOf<String, ResolvedComponentWithArtifacts>(
                    "org.jetbrains.kotlin:kotlin-dom-api-compat:${consumer.kotlinToolingVersion}" to ResolvedComponentWithArtifacts(
                        configuration = "commonFakeApiElements-published",
                        artifacts = mutableListOf()
                    ),
                ).prettyPrinted,
                it.resolveProjectDependencyComponentsWithArtifacts().prettyPrinted
            )
        }

        listOf(
            consumer.multiplatformExtension.js().compilations.getByName("main").internal.configurations.compileDependencyConfiguration,
            /**
             * This weird resolution happens because of KotlinPlatformType.CompatibilityRule. Was it used to allow consuming pre-metadata
             * variants when metadata publication was introduced? In dom-api-compat case there are 2 compatible variants; could it lead to
             * a weird ambiguity failure in the future?
             */
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration,
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration,
        ).forEach {
            assertEquals(
                mapOf<String, ResolvedComponentWithArtifacts>(
                    "org.jetbrains.kotlin:kotlin-dom-api-compat:${consumer.kotlinToolingVersion}" to ResolvedComponentWithArtifacts(
                        configuration = "jsApiElements-published",
                        artifacts = mutableListOf(
                            kmpJsVariantAttributes + klibArtifact,
                        )
                    ),
                ).prettyPrinted,
                it.resolveProjectDependencyComponentsWithArtifacts().prettyPrinted
            )
        }
    }

    @Test
    fun `uklib resolution - all configurations can resolve stdlib`() {
        val consumer = uklibConsumer {
            repositories.mavenLocal()
            repositories.mavenCentral()
            kotlin {
                iosArm64()
                iosX64()
                jvm()
                js()
                wasmJs()
                sourceSets.commonMain.dependencies { implementation("org.jetbrains.kotlin:kotlin-stdlib:${project.kotlinToolingVersion}") }
            }
        }

        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "org.jetbrains.kotlin:kotlin-stdlib:${consumer.kotlinToolingVersion}" to ResolvedComponentWithArtifacts(
                    configuration = "nativeApiElements",
                    artifacts = mutableListOf()
                ),
            ).prettyPrinted, consumer.multiplatformExtension.iosArm64().compilationResolution().prettyPrinted
        )
        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "org.jetbrains.kotlin:kotlin-stdlib:${consumer.kotlinToolingVersion}" to ResolvedComponentWithArtifacts(
                    configuration = "jvmApiElements",
                    artifacts = mutableListOf(
                        kmpJvmApiVariantAttributes + jarArtifact + notAMetadataJar,
                    )
                ),
                "org.jetbrains:annotations:13.0" to ResolvedComponentWithArtifacts(
                    configuration = "compile",
                    artifacts = mutableListOf(
                        jvmApiAttributes + jarArtifact + notAMetadataJar,
                    )
                )
            ).prettyPrinted, consumer.multiplatformExtension.jvm().compilationResolution().prettyPrinted
        )
        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "org.jetbrains.kotlin:kotlin-stdlib:${consumer.kotlinToolingVersion}" to ResolvedComponentWithArtifacts(
                    configuration = "wasmJsApiElements",
                    artifacts = mutableListOf(
                    )
                ),
                "org.jetbrains.kotlin:kotlin-stdlib-wasm-js:${consumer.kotlinToolingVersion}" to ResolvedComponentWithArtifacts(
                    configuration = "wasmJsApiElements",
                    artifacts = mutableListOf(
                        kmpWasmJsVariantAttributes + klibArtifact + packed,
                    )
                )
            ).prettyPrinted, consumer.multiplatformExtension.wasmJs().compilationResolution().prettyPrinted
        )
        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "org.jetbrains.kotlin:kotlin-stdlib:${consumer.kotlinToolingVersion}" to ResolvedComponentWithArtifacts(
                    configuration = "jsApiElements",
                    artifacts = mutableListOf(
                    )
                ),
                "org.jetbrains.kotlin:kotlin-stdlib-js:${consumer.kotlinToolingVersion}" to ResolvedComponentWithArtifacts(
                    configuration = "jsApiElements",
                    artifacts = mutableListOf(
                        kmpJsVariantAttributes + klibArtifact + packed,
                    )
                )
            ).prettyPrinted, consumer.multiplatformExtension.js().compilationResolution().prettyPrinted
        )
        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                // stdlib is excluded from native metadata compilations
            ).prettyPrinted,
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts().prettyPrinted,
        )
        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "org.jetbrains.kotlin:kotlin-stdlib:${consumer.kotlinToolingVersion}" to ResolvedComponentWithArtifacts(
                    configuration = "metadataApiElements",
                    artifacts = mutableListOf(
                        kmpMetadataVariantAttributes + maybeAMetadataJar + jarArtifact + libraryElementsJar,
                    )
                ),
            ).prettyPrinted,
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts().prettyPrinted,
        )
    }

    @Test
    fun `uklib resolution - all configurations can resolve direct POM-only JVM dependencies`() {
        val repo = generateMockRepository(
            tmpDir,
            mavenComponents = listOf(
                directMavenComponent()
            )
        )

        val consumer = uklibConsumer {
            repositories.maven(repo)
            kotlin {
                iosArm64()
                iosX64()
                jvm()
                js()
                sourceSets.commonMain.dependencies { implementation("foo:direct:1.0") }
            }
        }

        listOf(
            consumer.multiplatformExtension.iosArm64().compilationResolution(),
            consumer.multiplatformExtension.jvm().compilationResolution(),
            consumer.multiplatformExtension.js().compilationResolution(),
        ).forEach {
            assertEquals(
                mapOf<String, ResolvedComponentWithArtifacts>(
                    "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                        configuration = "compile",
                        artifacts = mutableListOf(
                            jvmApiAttributes + jarArtifact + notAMetadataJar,
                        )
                    )
                ).prettyPrinted,
                it.prettyPrinted
            )
        }

        listOf(
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts(),
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts(),
        ).forEach {
            assertEquals(
                mapOf<String, ResolvedComponentWithArtifacts>(
                    "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                        configuration = "compile",
                        artifacts = mutableListOf(
                            jvmApiAttributes + jarArtifact + maybeAMetadataJar,
                        )
                    )
                ).prettyPrinted,
                it.prettyPrinted,
            )
        }
    }

    @Test
    fun `uklib resolution - all configurations can resolve direct Gradle JVM dependencies`() {
        val repo = generateMockRepository(
            tmpDir,
            gradleComponents = listOf(
                GradleComponent(
                    GradleMetadataComponent(
                        component = directGradleComponent,
                        variants = listOf(
                            jvmApiVariant,
                        ),
                    ),
                    directMavenComponent("jar"),
                )
            ),
        )

        val consumer = uklibConsumer {
            repositories.maven(repo)
            kotlin {
                iosArm64()
                iosX64()
                jvm()
                js()
                sourceSets.commonMain.dependencies { implementation("foo:direct:1.0") }
            }
        }

        listOf(
            consumer.multiplatformExtension.iosArm64().compilationResolution(),
            consumer.multiplatformExtension.jvm().compilationResolution(),
            consumer.multiplatformExtension.js().compilationResolution(),
        ).forEach {
            assertEquals(
                mapOf<String, ResolvedComponentWithArtifacts>(
                    "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                        configuration = "jvmApiElements",
                        artifacts = mutableListOf(
                            jvmApiAttributes + jarArtifact + notAMetadataJar,
                        )
                    )
                ).prettyPrinted,
                it.prettyPrinted,
            )
        }

        listOf(
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts(),
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts(),
        ).forEach {
            assertEquals(
                mapOf<String, ResolvedComponentWithArtifacts>(
                    "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                        configuration = "jvmApiElements",
                        artifacts = mutableListOf(
                            jvmApiAttributes + jarArtifact + maybeAMetadataJar,
                        )
                    )
                ).prettyPrinted,
                it.prettyPrinted,
            )
        }
    }

    @Test
    fun `uklib resolution - KMP and Uklib variants in a single component`() {
        val repo = generateMockRepository(
            tmpDir,
            listOf(
                GradleComponent(
                    GradleMetadataComponent(
                        component = directGradleComponent,
                        variants = listOf(
                            kmpMetadataJarVariant,
                            kmpIosArm64MetadataJarVariant,
                            kmpIosArm64KlibVariant,
                            kmpJvmApiVariant,
                            uklibApiVariant,
                        ),
                    ),
                    directMavenComponent(),
                ),
            )
        )

        val consumer = uklibConsumer {
            repositories.maven(repo)
            kotlin {
                iosArm64()
                iosX64()
                jvm()
                js()
                sourceSets.commonMain.dependencies { implementation("foo:direct:1.0") }
            }
        }

        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    configuration = uklibApiVariant.name,
                    artifacts = mutableListOf(uklibApiVariant.attributes + uklibTransformationIosArm64Attributes)
                ),
            ).prettyPrinted, consumer.multiplatformExtension.iosArm64().compilationResolution().prettyPrinted
        )
        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    configuration = uklibApiVariant.name,
                    artifacts = mutableListOf(
                        /**
                         * Artifacts are filtered by [UnzippedUklibToPlatformCompilationTransform]
                         */
                    )
                ),
            ).prettyPrinted, consumer.multiplatformExtension.iosX64().compilationResolution().prettyPrinted
        )
        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    configuration = uklibApiVariant.name,
                    artifacts = mutableListOf(uklibApiVariant.attributes + uklibTransformationJvmAttributes)
                ),
            ).prettyPrinted, consumer.multiplatformExtension.jvm().compilationResolution().prettyPrinted
        )
        listOf(
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts(),
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts(),
        ).forEach {
            assertEquals(
                mapOf<String, ResolvedComponentWithArtifacts>(
                    "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                        configuration = uklibApiVariant.name,
                        artifacts = mutableListOf(uklibApiVariant.attributes + uklibTransformationMetadataAttributes)
                    ),
                ).prettyPrinted,
                it.prettyPrinted,
            )
        }
    }

    @Test
    fun `standard kmp resolution - KMP and Uklib variants in a single component`() {
        val repo = generateMockRepository(
            tmpDir,
            listOf(
                GradleComponent(
                    GradleMetadataComponent(
                        component = directGradleComponent,
                        variants = listOf(
                            kmpMetadataJarVariant,
                            kmpIosArm64MetadataJarVariant,
                            kmpIosArm64KlibVariant,
                            kmpJvmApiVariant,
                            uklibApiVariant,
                        ),
                    ),
                    directMavenComponent(),
                ),
            )
        )

        val consumer = uklibConsumer(
            resolutionStrategy = KmpResolutionStrategy.StandardKMPResolution,
        ) {
            repositories.maven(repo)
            kotlin {
                iosArm64()
                iosX64()
                jvm()
                js()
                sourceSets.commonMain.dependencies { implementation("foo:direct:1.0") }
            }
        }

        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    configuration = kmpIosArm64KlibVariant.name,
                    artifacts = mutableListOf(
                        kmpIosArm64KlibVariant.attributes + klibCinteropCommonizerType,
                    )
                ),
            ).prettyPrinted,
            consumer.multiplatformExtension.iosArm64().compilationResolution().prettyPrinted
        )
        assertEquals(
            mapOf<String, ResolvedComponentWithArtifacts>(
                "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                    configuration = kmpJvmApiVariant.name,
                    artifacts = mutableListOf(
                        kmpJvmApiVariant.attributes + jarArtifact,
                    )
                ),
            ).prettyPrinted,
            consumer.multiplatformExtension.jvm().compilationResolution().prettyPrinted
        )
        listOf(
            consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts(),
            consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts()
        ).forEach {
            assertEquals(
                mapOf<String, ResolvedComponentWithArtifacts>(
                    "foo:direct:1.0" to ResolvedComponentWithArtifacts(
                        configuration = kmpMetadataJarVariant.name,
                        artifacts = mutableListOf(
                            kmpMetadataJarVariant.attributes + jarArtifact + libraryElementsJar,
                        )
                    ),
                ).prettyPrinted,
                it.prettyPrinted
            )
        }
        val iosX64ResolutionExceptions = findMatchingExceptions(
            runCatching {
                consumer.multiplatformExtension.iosX64().compilationResolution()
            }.exceptionOrNull() ?: error("Expect a failure"),
            VariantSelectionByAttributesException::class.java
        )
        assert(iosX64ResolutionExceptions.size == 1) {
            iosX64ResolutionExceptions
        }
    }

    @Test
    fun `standard kmp resolution - can't resolve Uklib variant`() {
        val repo = generateMockRepository(
            tmpDir,
            listOf(
                GradleComponent(
                    GradleMetadataComponent(
                        component = directGradleComponent,
                        variants = listOf(
                            uklibApiVariant,
                        ),
                    ),
                    directMavenComponent(),
                ),
            )
        )

        val consumer = uklibConsumer(
            resolutionStrategy = KmpResolutionStrategy.StandardKMPResolution,
        ) {
            repositories.maven(repo)
            kotlin {
                iosArm64()
                iosX64()
                jvm()
                js()
                sourceSets.commonMain.dependencies { implementation("foo:direct:1.0") }
            }
        }

        listOf(
            { consumer.multiplatformExtension.iosArm64().compilationResolution() },
            { consumer.multiplatformExtension.jvm().compilationResolution() },
            { consumer.multiplatformExtension.sourceSets.iosMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts() },
            { consumer.multiplatformExtension.sourceSets.commonMain.get().internal.resolvableMetadataConfiguration.resolveProjectDependencyComponentsWithArtifacts() },
        ).forEach {
            val resolutionException = runCatching {
                it()
            }.exceptionOrNull() ?: error("Expect a failure")
            findMatchingExceptions(resolutionException, VariantSelectionByAttributesException::class.java).single()
        }
    }

    private fun <T : Throwable> findMatchingExceptions(
        topLevelException: Throwable,
        targetClass: Class<T>,
    ): Set<T> {
        val exceptionsStack = mutableListOf(topLevelException)
        val walkedExceptions = mutableSetOf<Throwable>()
        val matchingExceptions = mutableSetOf<T>()
        while (exceptionsStack.isNotEmpty()) {
            val current = exceptionsStack.removeLast()
            if (current in walkedExceptions) continue
            walkedExceptions.add(current)
            if (targetClass.isInstance(current)) {
                @Suppress("UNCHECKED_CAST")
                matchingExceptions.add(current as T)
            }
            exceptionsStack.addAll(
                when (current) {
                    is MultiCauseException -> current.causes.mapNotNull { it }
                    else -> listOfNotNull(current.cause)
                }
            )
        }
        return matchingExceptions
    }

    /**
     * FIXME: Test runtime resolvable configurations
     */

    /**
     * FIXME: Figure out a set of tests for AGP dependencies. Take into account:
     * - KMP style AGP publication vs pure AGP library publication. "libraryelements" is only used in pure AGP and "jvm.environment" + "kotlin.platform.type" only in KMP
     * - Flavors and build types
     * - publishLibraryVariantsGroupedByFlavor
     */

    fun `uklib resolution - stdlib-common and other legacy`() {
        // Do we need this?
    }

    fun `uklib resolution - all platform specific configurations can resolve all other platform specific kmp variants`() {
        // Or maybe these should be explicitly unsupported?
    }

    private fun uklibConsumer(
        resolutionStrategy: KmpResolutionStrategy = KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs,
        code: Project.() -> Unit = {},
    ): Project {
        return buildProjectWithMPP(
            preApplyCode = {
                when (resolutionStrategy) {
                    KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs -> {
                        setUklibResolutionStrategy(KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs)
                    }
                    KmpResolutionStrategy.StandardKMPResolution -> {}
                }
                // Test stdlib in a separate test
                enableDefaultStdlibDependency(false)
                enableDefaultJsDomApiDependency(false)
            },
            code = code,
        ).evaluate()
    }

    private val transitiveGradleComponent = GradleMetadataComponent.Component(
        group = "foo",
        module = "transitive",
        version = "1.0",
    )

    private fun transitiveMavenComponent(packaging: String? = null) = MavenComponent(
        transitiveGradleComponent.group, transitiveGradleComponent.module, transitiveGradleComponent.version,
        packaging = packaging,
        dependencies = listOf(),
        true,
    )

    private val directGradleComponent = GradleMetadataComponent.Component(
        group = "foo",
        module = "direct",
        version = "1.0",
    )

    private fun directMavenComponent(packaging: String? = null) = MavenComponent(
        directGradleComponent.group, directGradleComponent.module, directGradleComponent.version,
        packaging = packaging,
        dependencies = listOf(),
        true,
    )

    private val uklibApiVariant = Variant(
        name = "uklibApiElements",
        attributes = mapOf(
            "org.gradle.usage" to "kotlin-uklib-api",
            "org.gradle.category" to "library",
            "org.jetbrains.kotlin.uklib" to "true",
        ),
        files = listOf(
            GradleMetadataComponent.MockVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "uklib",
                type = GradleMetadataComponent.MockVariantType.UklibArchive { temporaryDirectory ->
                    setOf(
                        UklibFragment(
                            identifier = "iosArm64Main",
                            attributes = setOf("ios_arm64"),
                            file = temporaryDirectory.resolve("iosArm64Main").also {
                                it.createDirectory()
                                it.resolve(".keep").createNewFile()
                            }
                        ),
                        UklibFragment(
                            identifier = "commonMain",
                            attributes = setOf("ios_arm64", "jvm"),
                            file = temporaryDirectory.resolve("commonMain").also {
                                it.createDirectory()
                                it.resolve(".keep").createNewFile()
                            }
                        ),
                        UklibFragment(
                            identifier = "jvmMain",
                            attributes = setOf("jvm"),
                            file = temporaryDirectory.resolve("jvmMain").also {
                                it.createDirectory()
                                it.resolve(".keep").createNewFile()
                            }
                        ),
                    )
                }
            )
        ),
        dependencies = listOf()
    )

    private val jvmApiVariant = Variant(
        name = "jvmApiElements",
        attributes = jvmApiAttributes,
        files = listOf(
            GradleMetadataComponent.MockVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "jar",
                classifier = "jvm"
            )
        ),
        dependencies = listOf()
    )

    private val kmpMetadataJarVariant = kmpMetadataJarVariant()
    private fun kmpMetadataJarVariant(dependencies: List<GradleMetadataComponent.Dependency> = listOf()) = Variant(
        name = "metadataApiElements",
        attributes = kmpMetadataVariantAttributes,
        files = listOf(
            GradleMetadataComponent.MockVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "jar",
                type = GradleMetadataComponent.MockVariantType.MetadataJar,
            )
        ),
        dependencies = dependencies
    )

    private val kmpIosArm64MetadataJarVariant = kmpIosArm64MetadataJarVariant()
    private fun kmpIosArm64MetadataJarVariant(dependencies: List<GradleMetadataComponent.Dependency> = listOf()) = Variant(
        name = "iosArm64MetadataElements-published",
        attributes = kmpIosArm64MetadataVariantAttributes,
        files = listOf(
            GradleMetadataComponent.MockVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "jar",
                classifier = "ios_arm64",
                type = GradleMetadataComponent.MockVariantType.MetadataJar,
            )
        ),
        dependencies = dependencies
    )

    private val kmpIosX64MetadataJarVariant = kmpIosX64MetadataJarVariant()
    private fun kmpIosX64MetadataJarVariant(dependencies: List<GradleMetadataComponent.Dependency> = listOf()) = Variant(
        name = "iosX64MetadataElements-published",
        attributes = kmpIosX64MetadataVariantAttributes,
        files = listOf(
            GradleMetadataComponent.MockVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "jar",
                classifier = "ios_x64",
                type = GradleMetadataComponent.MockVariantType.MetadataJar,
            )
        ),
        dependencies = dependencies
    )

    private val kmpIosArm64KlibVariant = Variant(
        name = "iosArm64ApiElements-published",
        attributes = kmpIosArm64VariantAttributes,
        files = listOf(
            GradleMetadataComponent.MockVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "klib",
                classifier = "ios_arm64",
            )
        ),
        dependencies = listOf()
    )

    private val kmpIosX64KlibVariant = Variant(
        name = "iosX64ApiElements-published",
        attributes = kmpIosX64VariantAttributes,
        files = listOf(
            GradleMetadataComponent.MockVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "klib",
                classifier = "ios_x64",
            )
        ),
        dependencies = listOf()
    )

    private val kmpJvmApiVariant = Variant(
        name = "jvmApiElements-published",
        attributes = kmpJvmApiVariantAttributes,
        files = listOf(
            GradleMetadataComponent.MockVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "jar",
                classifier = "jvm",
            )
        ),
        dependencies = listOf()
    )
}